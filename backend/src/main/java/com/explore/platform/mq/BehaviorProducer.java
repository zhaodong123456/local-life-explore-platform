package com.explore.platform.mq;

import com.explore.platform.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 用户行为采集生产者
 * <p>
 * 发送行为消息到 topic 交换机 behavior.exchange（routingKey=behavior.collect）
 * 消费端异步落库，不阻塞主流程。
 *
 * @author explore-platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BehaviorProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送用户行为采集消息
     *
     * @param message 行为消息
     */
    public void send(BehaviorMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BEHAVIOR_EXCHANGE,
                    RabbitMQConfig.BEHAVIOR_ROUTING_KEY,
                    message);
            log.debug("行为消息已投递：userId={}, shopId={}, type={}",
                    message.getUserId(), message.getShopId(), message.getBehaviorType());
        } catch (Exception e) {
            // 行为采集失败不阻塞主流程，仅记录日志
            log.warn("行为消息投递失败：userId={}, shopId={}, type={}",
                    message.getUserId(), message.getShopId(), message.getBehaviorType(), e);
        }
    }
}
