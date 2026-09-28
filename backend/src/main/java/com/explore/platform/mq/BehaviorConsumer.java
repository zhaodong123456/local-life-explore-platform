package com.explore.platform.mq;

import com.explore.platform.config.RabbitMQConfig;
import com.explore.platform.entity.UserBehavior;
import com.explore.platform.mapper.UserBehaviorMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 用户行为采集消费者（手动 ack）
 * <p>
 * 接收到行为消息后写入 tb_user_behavior，失败时 nack 不重入队列（由重试/DLQ 兜底）。
 *
 * @author explore-platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BehaviorConsumer {

    private final UserBehaviorMapper userBehaviorMapper;

    @RabbitListener(queues = RabbitMQConfig.BEHAVIOR_QUEUE)
    public void onMessage(BehaviorMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            UserBehavior behavior = new UserBehavior();
            behavior.setUserId(message.getUserId());
            behavior.setShopId(message.getShopId());
            behavior.setBehaviorType(message.getBehaviorType());
            behavior.setWeight(message.getWeight());
            userBehaviorMapper.insert(behavior);
            log.debug("行为消息已消费落库：userId={}, shopId={}, type={}",
                    message.getUserId(), message.getShopId(), message.getBehaviorType());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("行为消息消费失败：userId={}, shopId={}, type={}",
                    message.getUserId(), message.getShopId(), message.getBehaviorType(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
