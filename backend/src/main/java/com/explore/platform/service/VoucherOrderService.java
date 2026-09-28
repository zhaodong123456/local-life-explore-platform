package com.explore.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.mq.SeckillOrderMessage;
import com.explore.platform.vo.VoucherOrderVO;

/**
 * 优惠券订单服务（秒杀下单 / 我的订单 / 模拟支付 / 商家核销）
 *
 * @author explore-platform
 */
public interface VoucherOrderService {

    /**
     * 秒杀下单：限流 → Lua 原子扣库存（一人一单）→ Redisson 锁 DB 兜底扣减 → MQ 异步创建订单。
     *
     * @param userId    当前登录用户
     * @param voucherId 秒杀券ID
     * @return 预生成的订单ID（订单由 MQ 消费者异步落库）
     */
    Long seckillVoucher(Long userId, Long voucherId);

    /**
     * MQ 消费入口：按消息异步创建订单（幂等，重复消息不产生重复订单）
     */
    void createOrderFromMessage(SeckillOrderMessage message);

    /**
     * 我的订单分页（下单时间倒序）
     */
    IPage<VoucherOrderVO> pageMyOrders(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 商家名下商铺的订单分页（核销记录页；可按状态过滤）
     *
     * @param status 可选，1未支付 2已支付待核销 3已核销 4已退款 5已关闭
     */
    IPage<VoucherOrderVO> pageMerchantOrders(Long merchantUserId, Integer pageNum, Integer pageSize,
                                             Integer status);

    /**
     * 模拟支付：仅本人 + 未支付订单可操作
     */
    void pay(Long userId, Long orderId);

    /**
     * 商家核销：仅订单所属商铺的商家可操作，且订单须为"已支付待核销"
     */
    void writeOff(Long merchantUserId, Long orderId);
}
