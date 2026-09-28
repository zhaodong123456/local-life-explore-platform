package com.explore.platform.controller;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.explore.platform.common.exception.BusinessException;
import com.explore.platform.entity.SeckillVoucher;
import com.explore.platform.entity.Voucher;
import com.explore.platform.entity.VoucherOrder;
import com.explore.platform.mapper.SeckillVoucherMapper;
import com.explore.platform.mapper.VoucherMapper;
import com.explore.platform.mapper.VoucherOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 【仅压测基线用】同步 DB 下单接口 —— 代表"未做 Redis+MQ 优化"的传统写法，
 * 仅在 benchmark profile 下生效（启动参数 --spring.profiles.active=benchmark）。
 * <p>
 * 与优化版（VoucherOrderServiceImpl#seckillVoucher）的差异：
 * 1. 每次请求直接查 DB 库存（无 Redis 预热/Lua 原子扣减）；
 * 2. 扣减+插订单在同一线程同步完成（无 MQ 异步削峰）；
 * 3. user_id 使用随机数避开 uk_user_voucher 唯一键，
 *    保证压测中每个请求都走完整下单链路（业务上仅用于基准对比，不代表真实业务语义）。
 * <p>
 * 压测后需清理该接口产生的订单（voucher_id 对应的 benchmark 券）。
 *
 * @author explore-platform
 */
@Profile("benchmark")
@RestController
@RequestMapping("/api/bench")
@RequiredArgsConstructor
public class BenchmarkSeckillController {

    private final VoucherMapper voucherMapper;

    private final SeckillVoucherMapper seckillVoucherMapper;

    private final VoucherOrderMapper voucherOrderMapper;

    @PostMapping("/seckill/{voucherId}")
    public Long benchSeckill(@PathVariable Long voucherId) {
        // 1. 查券与库存（直接打 DB）
        Voucher voucher = voucherMapper.selectById(voucherId);
        if (voucher == null) {
            throw new BusinessException("券不存在");
        }
        SeckillVoucher seckill = seckillVoucherMapper.selectById(voucherId);
        if (seckill == null || seckill.getStock() <= 0) {
            throw new BusinessException("库存不足");
        }
        // 2. 条件扣减（stock>0 防负数，但查询与扣减之间存在并发窗口——正是未优化版本的形态）
        int rows = seckillVoucherMapper.update(null, new UpdateWrapper<SeckillVoucher>()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0));
        if (rows == 0) {
            throw new BusinessException("库存不足");
        }
        // 3. 同步插订单（随机 user_id 避开唯一键，压测语义见类注释）
        VoucherOrder order = new VoucherOrder();
        order.setId(IdWorker.getId());
        long benchUserId = 900_000_000L + java.util.concurrent.ThreadLocalRandom.current().nextLong(1_000_000_000L);
        order.setUserId(benchUserId);
        order.setVoucherId(voucherId);
        order.setShopId(voucher.getShopId());
        order.setOrderType(1);
        order.setStatus(1);
        voucherOrderMapper.insert(order);
        return order.getId();
    }
}
