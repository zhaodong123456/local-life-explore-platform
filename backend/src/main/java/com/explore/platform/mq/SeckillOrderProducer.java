package com.explore.platform.mq;

import com.explore.platform.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 秒杀下单消息生产者
 * <p>
 * 削峰：接口线程只做"限流 → Lua 扣库存 → 发消息"，写库压力转移到消费者匀速处理。
 * 生产者确认回调记录在应用启动配置（publisher-confirm-type: correlated），
 * ConfirmCallback 的精细化处理（失败重投/落本地消息表）在阶段4-1完善。
 *
 * @author explore-platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送秒杀下单消息（同步等待 broker 接收，失败抛异常由调用方触发补偿）
     */
    public void send(SeckillOrderMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SECKILL_ORDER_EXCHANGE,
                    RabbitMQConfig.SECKILL_ORDER_ROUTING_KEY,
                    message);
            log.info("秒杀下单消息已投递：orderId={}, userId={}, voucherId={}",
                    message.getOrderId(), message.getUserId(), message.getVoucherId());
        } catch (Exception e) {
            // 投递失败必须抛给调用方做库存补偿，否则会出现"扣了库存但没有订单"
            log.error("秒杀下单消息投递失败：orderId={}", message.getOrderId(), e);
            throw e;
        }
    }
}
