package com.explore.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.explore.platform.cache.CacheKeys;
import com.explore.platform.common.exception.BusinessException;
import com.explore.platform.dto.VoucherRequest;
import com.explore.platform.entity.SeckillVoucher;
import com.explore.platform.entity.Shop;
import com.explore.platform.entity.Voucher;
import com.explore.platform.mapper.SeckillVoucherMapper;
import com.explore.platform.mapper.ShopMapper;
import com.explore.platform.mapper.VoucherMapper;
import com.explore.platform.service.MerchantService;
import com.explore.platform.service.VoucherService;
import com.explore.platform.vo.VoucherVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;// Redis 客户端，用于操作 Redis 数据库
import org.springframework.beans.BeanUtils;// Bean 工具类，用于复制属性
import org.springframework.data.redis.core.StringRedisTemplate;// 字符串模板，用于操作 Redis 字符串类型数据
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 优惠券服务实现
 *
 * @author explore-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherMapper voucherMapper;

    private final SeckillVoucherMapper seckillVoucherMapper;

    private final ShopMapper shopMapper;

    private final MerchantService merchantService;

    private final StringRedisTemplate stringRedisTemplate;

    private final RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createVoucher(Long userId, VoucherRequest request) {
        // 校验目标商铺存在且归当前商家所有
        ensureShopOwned(userId, request.getShopId());

        Voucher voucher = new Voucher();
        BeanUtils.copyProperties(request, voucher);
        // 新券默认下架，商家确认后手动上架
        voucher.setStatus(0);
        if (request.getType() == 1) {
            validateSeckillFields(request);// 校验秒杀券字段
        }
        voucherMapper.insert(voucher);

        // 秒杀券：同步写入库存扩展表
        if (request.getType() == 1) {
            SeckillVoucher seckill = new SeckillVoucher();
            seckill.setVoucherId(voucher.getId());
            seckill.setInitialStock(request.getStock());
            seckill.setStock(request.getStock());
            seckill.setBeginTime(request.getSeckillBegin());
            seckill.setEndTime(request.getSeckillEnd());
            seckillVoucherMapper.insert(seckill);
            // 新券尚未开售（默认下架），先以 DB 库存初始化 Redis 缓存值，上架接口会再次确保预热
            warmSeckillStock(voucher.getId(), request.getStock());
        }
        return voucher.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVoucher(Long userId, Long voucherId, VoucherRequest request) {
        Voucher voucher = getOwnedVoucher(userId, voucherId);
        if (request.getType() != null && !request.getType().equals(voucher.getType())) {
            throw new BusinessException("不允许修改券类型");
        }
        BeanUtils.copyProperties(request, voucher);
        voucherMapper.updateById(voucher);

        // 秒杀券同步更新扩展信息（库存调整在阶段3通过 Redis 处理，此处只更新时间/初始配置）
        if (Integer.valueOf(1).equals(voucher.getType())) {
            SeckillVoucher seckill = seckillVoucherMapper.selectById(voucherId);
            if (seckill != null) {
                seckill.setBeginTime(request.getSeckillBegin());
                seckill.setEndTime(request.getSeckillEnd());
                seckillVoucherMapper.updateById(seckill);
            }
        }
    }

    @Override
    public void updateVoucherStatus(Long userId, Long voucherId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("券状态参数非法");
        }
        Voucher voucher = getOwnedVoucher(userId, voucherId);
        voucher.setStatus(status);
        voucherMapper.updateById(voucher);

        // 秒杀券上下架需同步 Redis 库存，保证缓存与 DB 事实源一致
        if (Integer.valueOf(1).equals(voucher.getType())) {
            if (status == 1) {
                // 上架：确保 Redis 有库存（已存在则不覆盖，防止把进行中的扣减结果重置）
                SeckillVoucher seckill = seckillVoucherMapper.selectById(voucherId);
                if (seckill != null) {
                    warmSeckillStock(voucherId, seckill.getStock());
                }
            } else {
                // 下架：持锁清理 Redis 库存与下单标记，避免与正在进行的秒杀请求交叉
                clearSeckillStockWithLock(voucherId);
            }
        }
    }

    @Override
    public List<VoucherVO> listByShop(Long shopId) {
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<Voucher>()
                .eq(Voucher::getShopId, shopId)
                // 公开只看上架券
                .eq(Voucher::getStatus, 1)
                .orderByDesc(Voucher::getCreateTime);
        return voucherMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public IPage<VoucherVO> pageMerchantVouchers(Long userId, Integer pageNum, Integer pageSize, Long shopId) {
        int finalPageSize = Math.min(pageSize, 100);
        // 归属校验：商家只能管理自己名下商铺的券
        Long merchantId = merchantService.requireMerchantId(userId);
        List<Long> shopIds = shopMapper.selectList(new LambdaQueryWrapper<Shop>()
                        .select(Shop::getId)
                        .eq(Shop::getMerchantId, merchantId))
                .stream().map(Shop::getId).toList();
        if (shopIds.isEmpty()) {
            return new Page<>(pageNum, finalPageSize);
        }
        LambdaQueryWrapper<Voucher> wrapper = new LambdaQueryWrapper<Voucher>()
                // 含下架券：管理页需要看到全量状态
                .in(Voucher::getShopId, shopIds)
                .orderByDesc(Voucher::getCreateTime);
        if (shopId != null) {
            wrapper.eq(Voucher::getShopId, shopId);
        }
        Page<Voucher> page = voucherMapper.selectPage(new Page<>(pageNum, finalPageSize), wrapper);
        // 批量查商铺名，避免循环 N+1
        Map<Long, String> shopNameMap = shopMapper.selectBatchIds(shopIds).stream()
                .collect(Collectors.toMap(Shop::getId, Shop::getName, (a, b) -> a));
        return page.convert(v -> {
            VoucherVO vo = toVO(v);
            vo.setShopName(shopNameMap.get(v.getShopId()));
            return vo;
        });
    }

    /**
     * 校验商铺归当前商家所有
     */
    private void ensureShopOwned(Long userId, Long shopId) {
        Long merchantId = merchantService.requireMerchantId(userId);
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null || !shop.getMerchantId().equals(merchantId)) {
            throw new BusinessException("无权在该商铺发布优惠券");
        }
    }

    /**
     * 获取优惠券并校验归属（券 → 商铺 → 商家）
     */
    private Voucher getOwnedVoucher(Long userId, Long voucherId) {
        Long merchantId = merchantService.requireMerchantId(userId);// 获取当前用户已审核通过的商家ID
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            throw new BusinessException("优惠券不存在");
        }
        Shop shop = shopMapper.selectById(voucher.getShopId());
        if (shop == null || !shop.getMerchantId().equals(merchantId)) {
            throw new BusinessException("无权操作该优惠券");
        }
        return voucher;
    }

    private void validateSeckillFields(VoucherRequest request) {
        if (request.getStock() == null || request.getStock() <= 0) {
            throw new BusinessException("秒杀券库存必须大于0");
        }
        if (request.getSeckillBegin() == null || request.getSeckillEnd() == null) {
            throw new BusinessException("秒杀券必须设置活动时间");
        }
        if (!request.getSeckillEnd().isAfter(request.getSeckillBegin())) {
            throw new BusinessException("秒杀结束时间必须晚于开始时间");
        }
    }

    /**
     * 秒杀库存预热到 Redis（setIfAbsent：已存在不覆盖，避免重置进行中的扣减结果）
     */
    private void warmSeckillStock(Long voucherId, Integer stock) {
        try {
            stringRedisTemplate.opsForValue().setIfAbsent(
                    CacheKeys.seckillStockKey(voucherId), String.valueOf(stock));
        } catch (Exception e) {
            // Redis 预热失败不阻断主流程：秒杀接口侧会惰性预热兜底
            log.warn("秒杀券库存预热失败，等待接口惰性预热：voucherId={}", voucherId, e);
        }
    }

    /**
     * 下架时清理 Redis 库存与下单标记：按券加分布式锁，避免与正在执行的 Lua 扣减交叉。
     * （锁等待 3s，拿不到则跳过清理——库存 key 还在但券已下架，接口会在状态校验处拦截）
     */
    private void clearSeckillStockWithLock(Long voucherId) {
        RLock lock = redissonClient.getLock(CacheKeys.seckillLock(voucherId));
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (locked) {
                stringRedisTemplate.delete(CacheKeys.seckillStockKey(voucherId));
                stringRedisTemplate.delete(CacheKeys.seckillOrderSetKey(voucherId));
            } else {
                log.warn("下架清理秒杀缓存未拿到锁，跳过：voucherId={}", voucherId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("下架清理秒杀缓存失败：voucherId={}", voucherId, e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
    // 转换为优惠券VO
    private VoucherVO toVO(Voucher voucher) {
        VoucherVO vo = new VoucherVO();
        BeanUtils.copyProperties(voucher, vo);
        // 秒杀券附带库存
        if (voucher.getType() == 1) {
            SeckillVoucher seckill = seckillVoucherMapper.selectById(voucher.getId());
            if (seckill != null) {
                vo.setStock(seckill.getStock());
            }
        }
        return vo;
    }
}
