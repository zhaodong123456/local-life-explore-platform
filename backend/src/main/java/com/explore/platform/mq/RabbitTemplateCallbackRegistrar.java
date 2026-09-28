package com.explore.platform.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 生产者确认回调注册器（应用启动时注册）。
 * <p>
 * ConfirmCallback：Broker 收到消息并持久化后回调（ack=true），否则 ack=false；
 * ReturnsCallback：消息无法路由到队列时回调（如交换机存在但无绑定队列）。
 * <p>
 * 失败处理：当前仅记录 error 日志，由人工排查；阶段6 可加本地消息表做自动重投。
 *
 * @author explore-platform
 */
@Slf4j
@Component
public class RabbitTemplateCallbackRegistrar implements ApplicationListener<ContextRefreshedEvent> {

    private final RabbitTemplate rabbitTemplate;

    public RabbitTemplateCallbackRegistrar(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 生产者确认回调
        rabbitTemplate.setConfirmCallback((CorrelationData correlationData, boolean ack, String cause) -> {
            if (!ack) {
                log.error("消息投递到 Broker 失败：correlationData={}, cause={}", correlationData, cause);
            }
        });
        // 消息无法路由时回调
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("消息路由失败：exchange={}, routingKey={}, replyText={}, message={}",
                    returned.getExchange(), returned.getRoutingKey(), returned.getReplyText(),
                    returned.getMessage());
        });
        log.info("RabbitTemplate ConfirmCallback / ReturnsCallback 已注册");
    }
}
