package com.explore.platform.mq;

import com.explore.platform.config.RabbitMQConfig;
import com.explore.platform.service.VoucherOrderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 秒杀下单消息消费者：异步创建订单（削峰填谷，DB 写压力与接口流量解耦）。
 * <p>
 * 幂等双保险：
 * 1. 消费端落库前查 (user_id, voucher_id) 是否已有订单，有则直接确认（重复投递/重复消息丢弃）；
 * 2. tb_voucher_order.uk_user_voucher 唯一键数据库兜底，并发重复插入只会成功一条，
 *    命中 DuplicateKeyException 视为已消费，正常确认不报错。
 * <p>
 * 可靠性：
 * - 手动 ack：业务成功才确认，失败 basicNack 不重回队列；
 * - 重试：spring.rabbitmq.listener.simple.retry 最多 3 次；
 * - 死信：重试耗尽进入 DLX（阶段4-1 已建设 message.dlq.queue）。
 *
 * @author explore-platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderConsumer {// 秒杀下单消息消费者

    private final VoucherOrderService voucherOrderService;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_ORDER_QUEUE)// 监听秒杀下单消息队列
    // 处理秒杀下单消息
    //SeckillOrderMessage 秒杀下单消息
    //Channel RabbitMQ 通道
    //deliveryTag 消息 deliveryTag，用于手动 ack
    public void onMessage(SeckillOrderMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("消费秒杀下单消息：orderId={}, userId={}, voucherId={}",
                    message.getOrderId(), message.getUserId(), message.getVoucherId());
            voucherOrderService.createOrderFromMessage(message);
            channel.basicAck(deliveryTag, false);// 手动 ack，确认消息已处理
        } catch (Exception e) {
            log.error("秒杀下单消息消费失败：orderId={}, deliveryTag={}", message.getOrderId(), deliveryTag, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
