package com.explore.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.explore.platform.cache.CacheKeys;
import com.explore.platform.common.exception.BusinessException;
import com.explore.platform.common.result.ErrorCode;
import com.explore.platform.entity.SeckillVoucher;
import com.explore.platform.entity.Shop;
import com.explore.platform.entity.Voucher;
import com.explore.platform.entity.VoucherOrder;
import com.explore.platform.mapper.SeckillVoucherMapper;
import com.explore.platform.mapper.ShopMapper;
import com.explore.platform.mapper.VoucherMapper;
import com.explore.platform.mapper.VoucherOrderMapper;
import com.explore.platform.mq.SeckillOrderMessage;
import com.explore.platform.mq.SeckillOrderProducer;
import com.explore.platform.ratelimit.SeckillRateLimiter;
import com.explore.platform.service.MerchantService;
import com.explore.platform.service.VoucherOrderService;
import com.explore.platform.vo.VoucherOrderVO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 优惠券订单服务实现。
 * <p>
 * 【秒杀链路设计（与黑马点评的差异点）】
 * 1. 库存主战场放 Redis：Lua 脚本一次性完成"一人一单校验 + 库存校验 + 扣减 + 下单标记"，
 *    原子操作，不存在查改之间的并发窗口，从根上杜绝超卖；
 * 2. 接口线程零 DB 写：Lua 通过后只发 MQ 即返回，DB 条件扣减（stock > 0）移到消费者执行，
 *    接口吞吐不受 DB 行锁/分布式锁串行化拖累，削峰效果实测显著（见压测报告）；
 * 3. DB 兜底：消费者在 Redisson 锁内执行 DB 条件扣减，DB 是重启/故障时的事实源，
 *    多消费者并发安全（默认单消费者天然串行，锁保证扩容后依然正确）；
 * 4. 幂等双保险：Redis 下单标记（快速拦截重复点击）+ 表 uk_user_voucher 唯一键兜底；
 * 5. 对账补偿：消费端发现重复订单/DB 售罄等异常时回补 Redis 库存（该消息的 Lua 扣减是孤儿扣减），
 *    避免"扣了库存没有订单"；MQ 投递失败同样回滚 Redis 并允许用户立即重试。
 * <p>
 * 已知取舍（阶段4-1完善）：DB 扣减在消费端异步执行，存在"Redis 已扣、DB 未扣"的短暂窗口，
 * 活动期间以 Redis 为准，最终由消费端追平；进程崩溃导致的丢失通过消息表/定时对账补偿。
 *
 * @author explore-platform
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl implements VoucherOrderService {

    private final VoucherMapper voucherMapper;

    private final SeckillVoucherMapper seckillVoucherMapper;

    private final VoucherOrderMapper voucherOrderMapper;

    private final ShopMapper shopMapper;

    private final MerchantService merchantService;

    private final StringRedisTemplate stringRedisTemplate;

    private final RedissonClient redissonClient;

    private final SeckillOrderProducer seckillOrderProducer;

    private final SeckillRateLimiter rateLimiter;

    /**
     * 秒杀原子脚本（resources/script/seckill.lua）
     */
    private final DefaultRedisScript<Long> seckillScript;

    /**
     * 补偿脚本：回滚库存与下单标记（SREM 对已存在集合操作，TTL 保持不变）
     */
    private final DefaultRedisScript<Long> rollbackScript;

    public VoucherOrderServiceImpl(VoucherMapper voucherMapper,
                                   SeckillVoucherMapper seckillVoucherMapper,
                                   VoucherOrderMapper voucherOrderMapper,
                                   ShopMapper shopMapper,
                                   MerchantService merchantService,
                                   StringRedisTemplate stringRedisTemplate,
                                   RedissonClient redissonClient,
                                   SeckillOrderProducer seckillOrderProducer,
                                   SeckillRateLimiter rateLimiter) {
        this.voucherMapper = voucherMapper;
        this.seckillVoucherMapper = seckillVoucherMapper;
        this.voucherOrderMapper = voucherOrderMapper;
        this.shopMapper = shopMapper;
        this.merchantService = merchantService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient;
        this.seckillOrderProducer = seckillOrderProducer;
        this.rateLimiter = rateLimiter;

        this.seckillScript = new DefaultRedisScript<>();
        this.seckillScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("script/seckill.lua")));
        this.seckillScript.setResultType(Long.class);

        this.rollbackScript = new DefaultRedisScript<>();
        this.rollbackScript.setScriptText(
                "redis.call('incr', KEYS[1]); redis.call('srem', KEYS[2], ARGV[1]); return 1");
        this.rollbackScript.setResultType(Long.class);
    }

    @Override
    public Long seckillVoucher(Long userId, Long voucherId) {
        // ---------- 1. 接口本地限流（二级；一级在网关），超出直接 429 ----------
        if (!rateLimiter.tryAcquire(userId)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }

        // ---------- 2. 券与活动时间校验 ----------
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null || !Integer.valueOf(1).equals(voucher.getType())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "秒杀券不存在");
        }
        if (!Integer.valueOf(1).equals(voucher.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "优惠券未上架");
        }
        SeckillVoucher seckill = seckillVoucherMapper.selectById(voucherId);
        if (seckill == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "秒杀券不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(seckill.getBeginTime()) || now.isAfter(seckill.getEndTime())) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_IN_TIME);
        }

        // ---------- 3. 库存惰性预热到 Redis（上架时已预热则命中，不重复覆盖） ----------
        ensureStockPrewarm(voucherId, seckill.getStock());

        // ---------- 4. 预生成订单ID（消息与落库共用，作为幂等标识） ----------
        long orderId = IdWorker.getId();

        // ---------- 5. Lua 原子：一人一单校验 + 扣库存 + 下单标记 ----------
        String stockKey = CacheKeys.seckillStockKey(voucherId);
        String orderSetKey = CacheKeys.seckillOrderSetKey(voucherId);
        long setTtlSeconds = Math.max(60,
                Duration.between(now, seckill.getEndTime()).getSeconds() + 3600);
        Long luaResult = stringRedisTemplate.execute(seckillScript,
                List.of(stockKey, orderSetKey),
                String.valueOf(userId), String.valueOf(setTtlSeconds));
        if (luaResult == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        if (luaResult == -2) {
            throw new BusinessException(ErrorCode.SECKILL_REPEAT_ORDER);
        }
        if (luaResult == -1) {
            throw new BusinessException(ErrorCode.SECKILL_STOCK_EMPTY);
        }

        // ---------- 6. MQ 异步创建订单（削峰）；接口线程到此结束，不再触碰 DB 写 ----------
        // 投递失败回滚 Redis（库存+1、移除用户标记），用户可立即重试
        try {
            seckillOrderProducer.send(new SeckillOrderMessage(orderId, userId, voucherId));
        } catch (Exception e) {
            rollbackRedis(stockKey, orderSetKey, userId);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "秒杀下单失败，请重试");
        }
        return orderId;
    }

    /**
     * MQ 消费入口：异步完成"DB 条件扣减库存 + 落订单"，幂等优先。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrderFromMessage(SeckillOrderMessage message) {
        // 幂等校验：同一用户同一券已有订单（重复投递 / 重复消息），直接视为消费成功。
        // 注意：该消息在接口侧已执行过一次 Lua 扣减，属孤儿扣减，需回补 Redis 库存
        // （只加库存，不移除用户下单标记——原订单的标记必须保留）
        Long exists = voucherOrderMapper.selectCount(new LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getUserId, message.getUserId())
                .eq(VoucherOrder::getVoucherId, message.getVoucherId()));
        if (exists != null && exists > 0) {
            log.info("订单已存在，幂等丢弃并回补库存：userId={}, voucherId={}",
                    message.getUserId(), message.getVoucherId());
            refundRedisStock(message.getVoucherId());
            return;
        }
        Voucher voucher = voucherMapper.selectById(message.getVoucherId());
        if (voucher == null) {
            log.error("秒杀消息对应优惠券不存在，丢弃并回补库存：voucherId={}", message.getVoucherId());
            refundRedisStock(message.getVoucherId());
            return;
        }
        try {
            // DB 兜底扣减（Redisson 锁内条件更新，防超卖第二道防线）
            deductDbStockInConsumer(message.getVoucherId());
            VoucherOrder order = new VoucherOrder();
            order.setId(message.getOrderId());
            order.setUserId(message.getUserId());
            order.setVoucherId(message.getVoucherId());
            order.setShopId(voucher.getShopId());
            order.setOrderType(1);
            order.setStatus(1);
            voucherOrderMapper.insert(order);
        } catch (RuntimeException e) {
            // 扣减失败（DB 售罄）或命中唯一键：本事务回滚，同时回补 Redis 孤儿扣减
            refundRedisStock(message.getVoucherId());
            throw e;
        }
    }

    @Override
    public IPage<VoucherOrderVO> pageMyOrders(Long userId, Integer pageNum, Integer pageSize) {
        int finalPageSize = Math.min(pageSize, 100);
        Page<VoucherOrder> page = voucherOrderMapper.selectPage(new Page<>(pageNum, finalPageSize),
                new LambdaQueryWrapper<VoucherOrder>()
                        .eq(VoucherOrder::getUserId, userId)
                        .orderByDesc(VoucherOrder::getCreateTime));
        List<VoucherOrder> records = page.getRecords();
        if (records.isEmpty()) {
            return page.convert(o -> null);
        }
        // 批量查券与商铺，避免循环 N+1
        Map<Long, Voucher> voucherMap = voucherMapper.selectBatchIds(
                        records.stream().map(VoucherOrder::getVoucherId).distinct().toList())
                .stream().collect(Collectors.toMap(Voucher::getId, Function.identity()));
        Map<Long, Shop> shopMap = shopMapper.selectBatchIds(
                        records.stream().map(VoucherOrder::getShopId).distinct().toList())
                .stream().collect(Collectors.toMap(Shop::getId, Function.identity()));
        return page.convert(order -> {
            VoucherOrderVO vo = new VoucherOrderVO();
            vo.setId(String.valueOf(order.getId()));
            vo.setVoucherId(order.getVoucherId());
            vo.setShopId(order.getShopId());
            vo.setOrderType(order.getOrderType());
            vo.setStatus(order.getStatus());
            vo.setCreateTime(order.getCreateTime());
            vo.setPayTime(order.getPayTime());
            vo.setUseTime(order.getUseTime());
            Voucher voucher = voucherMap.get(order.getVoucherId());
            if (voucher != null) {
                vo.setVoucherTitle(voucher.getTitle());
                vo.setPayValue(voucher.getPayValue());
            }
            Shop shop = shopMap.get(order.getShopId());
            if (shop != null) {
                vo.setShopName(shop.getName());
            }
            return vo;
        });
    }

    @Override
    public IPage<VoucherOrderVO> pageMerchantOrders(Long merchantUserId, Integer pageNum, Integer pageSize,
                                                    Integer status) {
        int finalPageSize = Math.min(pageSize, 100);
        // 归属校验：商家只能查看/核销名下商铺的订单
        Long merchantId = merchantService.requireMerchantId(merchantUserId);
        List<Long> shopIds = shopMapper.selectList(new LambdaQueryWrapper<Shop>()
                        .select(Shop::getId)
                        .eq(Shop::getMerchantId, merchantId))
                .stream().map(Shop::getId).toList();
        if (shopIds.isEmpty()) {
            return new Page<>(pageNum, finalPageSize);
        }
        LambdaQueryWrapper<VoucherOrder> wrapper = new LambdaQueryWrapper<VoucherOrder>()
                .in(VoucherOrder::getShopId, shopIds)
                .orderByDesc(VoucherOrder::getCreateTime);
        if (status != null) {
            wrapper.eq(VoucherOrder::getStatus, status);
        }
        Page<VoucherOrder> page = voucherOrderMapper.selectPage(new Page<>(pageNum, finalPageSize), wrapper);
        List<VoucherOrder> records = page.getRecords();
        if (records.isEmpty()) {
            return page.convert(o -> null);
        }
        // 批量查券与商铺，避免循环 N+1
        Map<Long, Voucher> voucherMap = voucherMapper.selectBatchIds(
                        records.stream().map(VoucherOrder::getVoucherId).distinct().toList())
                .stream().collect(Collectors.toMap(Voucher::getId, Function.identity()));
        Map<Long, Shop> shopMap = shopMapper.selectBatchIds(
                        records.stream().map(VoucherOrder::getShopId).distinct().toList())
                .stream().collect(Collectors.toMap(Shop::getId, Function.identity()));
        return page.convert(order -> {
            VoucherOrderVO vo = new VoucherOrderVO();
            vo.setId(String.valueOf(order.getId()));
            vo.setVoucherId(order.getVoucherId());
            vo.setShopId(order.getShopId());
            vo.setOrderType(order.getOrderType());
            vo.setStatus(order.getStatus());
            vo.setCreateTime(order.getCreateTime());
            vo.setPayTime(order.getPayTime());
            vo.setUseTime(order.getUseTime());
            Voucher voucher = voucherMap.get(order.getVoucherId());
            if (voucher != null) {
                vo.setVoucherTitle(voucher.getTitle());
                vo.setPayValue(voucher.getPayValue());
            }
            Shop shop = shopMap.get(order.getShopId());
            if (shop != null) {
                vo.setShopName(shop.getName());
            }
            return vo;
        });
    }

    @Override
    public void pay(Long userId, Long orderId) {
        VoucherOrder order = getOwnOrder(userId, orderId);
        if (!Integer.valueOf(1).equals(order.getStatus())) {
            throw new BusinessException("订单状态不允许支付");
        }
        order.setStatus(2);
        order.setPayType(1);
        order.setPayTime(LocalDateTime.now());
        voucherOrderMapper.updateById(order);
    }

    @Override
    public void writeOff(Long merchantUserId, Long orderId) {
        VoucherOrder order = voucherOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        // 数据归属校验：订单 → 商铺 → 商家，禁止跨商家核销
        Long merchantId = merchantService.requireMerchantId(merchantUserId);
        Shop shop = shopMapper.selectById(order.getShopId());
        if (shop == null || !shop.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权核销其他商家的订单");
        }
        if (!Integer.valueOf(2).equals(order.getStatus())) {
            throw new BusinessException("订单未处于待核销状态");
        }
        order.setStatus(3);
        order.setUseTime(LocalDateTime.now());
        voucherOrderMapper.updateById(order);
    }

    /**
     * 库存惰性预热：仅当 Redis 无库存 key 时以 DB 值初始化（不覆盖进行中的扣减结果）
     */
    private void ensureStockPrewarm(Long voucherId, Integer dbStock) {
        try {
            stringRedisTemplate.opsForValue().setIfAbsent(
                    CacheKeys.seckillStockKey(voucherId), String.valueOf(dbStock));
        } catch (Exception e) {
            // Redis 预热失败不阻断请求，由 Lua 判 -1 走库存不足兜底（保守策略）
            log.warn("秒杀库存预热失败：voucherId={}", voucherId, e);
        }
    }

    /**
     * 消费端 DB 兜底扣减：Redisson 锁内条件更新（stock > 0），失败抛异常触发事务回滚。
     * 默认单消费者天然串行；锁保证未来扩容多消费者时扣减依然安全。
     */
    private void deductDbStockInConsumer(Long voucherId) {
        RLock lock = redissonClient.getLock(CacheKeys.seckillLock(voucherId));
        boolean locked = false;
        try {
            locked = lock.tryLock(2, 5, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
            int rows = seckillVoucherMapper.update(null,
                    new UpdateWrapper<SeckillVoucher>()
                            .setSql("stock = stock - 1")
                            .eq("voucher_id", voucherId)
                            .gt("stock", 0));
            if (rows == 0) {
                // DB 事实源已售罄而 Redis 仍有库存（Redis 被重置等异常场景），由调用方回补并告警
                log.error("DB 库存已售罄但 Redis 判定有库存，存在数据分歧：voucherId={}", voucherId);
                throw new BusinessException(ErrorCode.SECKILL_STOCK_EMPTY);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * Redis 补偿：MQ 投递失败 → 库存+1、移除下单标记（用户可立即重试）
     */
    private void rollbackRedis(String stockKey, String orderSetKey, long userId) {
        try {
            stringRedisTemplate.execute(rollbackScript,
                    List.of(stockKey, orderSetKey), String.valueOf(userId));
        } catch (Exception e) {
            log.error("秒杀 Redis 补偿失败，需人工核对：stockKey={}, userId={}", stockKey, userId, e);
        }
    }

    /**
     * 消费端回补孤儿扣减：只加库存、不移除用户下单标记。
     * 场景：重复消息 / DB 售罄丢弃——该消息在接口侧已执行过一次 Lua 扣减，需要归还。
     */
    private void refundRedisStock(Long voucherId) {
        try {
            String stockKey = CacheKeys.seckillStockKey(voucherId);
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
                stringRedisTemplate.opsForValue().increment(stockKey);
            }
        } catch (Exception e) {
            log.error("消费端 Redis 库存回补失败，需人工核对：voucherId={}", voucherId, e);
        }
    }

    /**
     * 查询订单并校验归属本人
     */
    private VoucherOrder getOwnOrder(Long userId, Long orderId) {
        VoucherOrder order = voucherOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作他人订单");
        }
        return order;
    }
}
