package com.explore.platform.mq;

import com.explore.platform.config.RabbitMQConfig;
import com.explore.platform.service.MessageService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 站内消息消费者（手动 ack + 幂等）。
 * <p>
 * 可靠性保证：
 * 1. 手动 ack：业务处理成功才 basicAck，失败则 basicNack（不重回队列）；
 * 2. 重试：spring.rabbitmq.listener.simple.retry 配置最多 3 次，间隔指数退避；
 * 3. 幂等：消费端先查 (receiver_user_id, biz_type, biz_id) 是否已存在，存在则直接 ack；
 * 4. 死信：重试耗尽仍失败 → 消息进入 message.dlq.queue，由人工/定时任务补偿。
 *
 * @author explore-platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyMessageConsumer {

    private final MessageService messageService;

    @RabbitListener(queues = RabbitMQConfig.MESSAGE_QUEUE)
    public void onMessage(NotifyMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("消费站内消息：type={}, receiver={}, bizType={}, bizId={}",
                    message.getType(), message.getReceiverUserId(), message.getBizType(), message.getBizId());

            // 落库（内部已做幂等：同一 bizType+bizId 重复触发不会产生重复消息）
            messageService.sendMessage(
                    message.getReceiverUserId(),
                    message.getType(),
                    message.getTitle(),
                    message.getContent(),
                    message.getBizType(),
                    message.getBizId());

            // 业务成功，手动确认
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("站内消息消费失败：type={}, receiver={}, deliveryTag={}",
                    message.getType(), message.getReceiverUserId(), deliveryTag, e);
            // 失败：拒绝且不重回队列，由 Retry 或 DLX 接管
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
