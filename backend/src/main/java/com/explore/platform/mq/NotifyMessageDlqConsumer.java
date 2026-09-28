package com.explore.platform.mq;

import com.explore.platform.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 站内消息死信队列消费者。
 * <p>
 * 接收重试耗尽仍失败的消息，记录日志并落库告警表（可选）。
 * 阶段6 可加定时任务扫描 DLQ 做人工补偿/重发。
 *
 * @author explore-platform
 */
@Slf4j
@Component
public class NotifyMessageDlqConsumer {

    @RabbitListener(queues = RabbitMQConfig.MESSAGE_DLQ)
    public void onDlqMessage(@Payload Object payload, Channel channel,
                             @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.error("【死信】站内消息进入 DLQ：payload={}, deliveryTag={}", payload, deliveryTag);
        // 死信消息确认，避免无限堆积
        channel.basicAck(deliveryTag, false);
    }
}
