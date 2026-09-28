package com.explore.platform.mq;

import com.explore.platform.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 站内消息生产者
 * <p>
 * 可靠性：publisher-confirm-type=correlated + publisher-returns=true，
 * 发送失败会触发 ConfirmCallback（应用启动类注册），此处仅负责投递。
 *
 * @author explore-platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送站内消息通知（topic 交换机，routingKey=message.notify）
     */
    public void send(NotifyMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MESSAGE_EXCHANGE,
                    RabbitMQConfig.MESSAGE_ROUTING_KEY,
                    message);
            log.info("站内消息已投递：type={}, receiver={}, bizType={}, bizId={}",
                    message.getType(), message.getReceiverUserId(), message.getBizType(), message.getBizId());
        } catch (Exception e) {
            log.error("站内消息投递失败：type={}, receiver={}", message.getType(), message.getReceiverUserId(), e);
            throw e;
        }
    }
}
