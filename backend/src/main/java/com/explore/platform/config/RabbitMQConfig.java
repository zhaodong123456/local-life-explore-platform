package com.explore.platform.config;

import org.springframework.amqp.core.Binding;//绑定关系
import org.springframework.amqp.core.BindingBuilder;//绑定关系构建器
import org.springframework.amqp.core.DirectExchange;//直接交换机
import org.springframework.amqp.core.Queue;//队列
import org.springframework.amqp.core.TopicExchange;//主题交换机
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;//消息转换器
import org.springframework.amqp.support.converter.MessageConverter;//消息转换器
import org.springframework.context.annotation.Bean;//Bean 注解，用于定义 Spring 容器中的 bean
import org.springframework.context.annotation.Configuration;//Configuration 注解，用于定义配置类

/**
 * RabbitMQ 配置
 * <p>
 * 拓扑：
 * 1. 秒杀下单：seckill.order.exchange(direct, 持久化) --seckill.order--> seckill.order.queue(持久化)
 * 2. 站内消息：message.exchange(topic, 持久化) --message.notify--> message.queue(持久化, 带DLX)
 *    - 消费失败重试 3 次后仍失败 → 拒绝不重回队列 → 进入 DLX
 *    - DLX: message.dlx(direct) --message.dlq--> message.dlq.queue(持久化)
 * <p>
 * 可靠性：
 * - 交换机/队列/消息全部持久化，Broker 重启不丢；
 * - 生产者确认：application.yml 已开 publisher-confirm-type: correlated + publisher-returns；
 * - 消费者手动 ack（application.yml listener.simple.acknowledge-mode: manual）；
 * - 消费失败重试：spring.rabbitmq.listener.simple.retry 配置（max-attempts: 3）；
 * - 死信队列：message.dlq.queue 接收重试耗尽/拒绝的消息，人工排查或定时补偿。
 *
 * @author explore-platform
 */
@Configuration
public class RabbitMQConfig {

    // ---------- 秒杀下单 ----------

    public static final String SECKILL_ORDER_EXCHANGE = "seckill.order.exchange";//秒杀下单交换机

    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";//秒杀下单队列   

    public static final String SECKILL_ORDER_ROUTING_KEY = "seckill.order";//秒杀下单路由键



    @Bean
    //DirectExchange 直接交换机，根据路由键将消息路由到指定队列
    public DirectExchange seckillOrderExchange() {
        return new DirectExchange(SECKILL_ORDER_EXCHANGE, true, false);//秒杀下单交换机
    }

    @Bean
    public Queue seckillOrderQueue() {
        return new Queue(SECKILL_ORDER_QUEUE, true);//秒杀下单队列
    }

    @Bean
    public Binding seckillOrderBinding(Queue seckillOrderQueue, DirectExchange seckillOrderExchange) {
        return BindingBuilder.bind(seckillOrderQueue)//绑定关系，将秒杀下单队列绑定到秒杀下单交换机，秒杀下单路由键
                .to(seckillOrderExchange)//绑定到秒杀下单交换机
                .with(SECKILL_ORDER_ROUTING_KEY);//秒杀下单路由键
    }

    // ---------- 站内消息通知 ----------

    public static final String MESSAGE_EXCHANGE = "message.exchange";

    public static final String MESSAGE_QUEUE = "message.queue";

    public static final String MESSAGE_ROUTING_KEY = "message.notify";

    // 死信交换机/队列
    public static final String MESSAGE_DLX = "message.dlx";

    public static final String MESSAGE_DLQ = "message.dlq";

    public static final String MESSAGE_DLQ_ROUTING_KEY = "message.dlq";

    // ---------- 用户行为采集 ----------

    public static final String BEHAVIOR_EXCHANGE = "behavior.exchange";

    public static final String BEHAVIOR_QUEUE = "behavior.collect.queue";

    public static final String BEHAVIOR_ROUTING_KEY = "behavior.collect";

    @Bean
    public TopicExchange messageExchange() {
        return new TopicExchange(MESSAGE_EXCHANGE, true, false);//站内消息交换机
    }

    @Bean
    public Queue messageQueue() {
        // 带死信交换机的消息队列：消费失败重试耗尽后进入 DLX
        return org.springframework.amqp.core.QueueBuilder.durable(MESSAGE_QUEUE)//持久化队列
                .withArgument("x-dead-letter-exchange", MESSAGE_DLX)//死信交换机
                .withArgument("x-dead-letter-routing-key", MESSAGE_DLQ_ROUTING_KEY)//死信路由键
                .build();//构建队列
    }

    @Bean
    public Binding messageBinding(Queue messageQueue, TopicExchange messageExchange) {
        return BindingBuilder.bind(messageQueue)//绑定关系，将站内消息队列绑定到站内消息交换机，站内消息路由键
                .to(messageExchange)//绑定到站内消息交换机
                .with(MESSAGE_ROUTING_KEY);//站内消息路由键
    }

    // 死信交换机与队列
    @Bean
    public DirectExchange messageDlx() {
        return new DirectExchange(MESSAGE_DLX, true, false);//死信交换机
    }

    @Bean
    public Queue messageDlq() {
        return new Queue(MESSAGE_DLQ, true);//死信队列
    }

    @Bean
    public Binding messageDlqBinding(Queue messageDlq, DirectExchange messageDlx) {
        return BindingBuilder.bind(messageDlq)//绑定关系，将死信队列绑定到死信交换机，死信路由键
                .to(messageDlx)//绑定到死信交换机
                .with(MESSAGE_DLQ_ROUTING_KEY);//死信路由键
    }

    // ---------- 用户行为采集 ----------

    @Bean
    public TopicExchange behaviorExchange() {
        return new TopicExchange(BEHAVIOR_EXCHANGE, true, false);//用户行为采集交换机
    }

    @Bean
    public Queue behaviorQueue() {
        return new Queue(BEHAVIOR_QUEUE, true);//用户行为采集队列
    }

    @Bean
    public Binding behaviorBinding(Queue behaviorQueue, TopicExchange behaviorExchange) {
        return BindingBuilder.bind(behaviorQueue)//绑定关系，将用户行为采集队列绑定到用户行为采集交换机，用户行为采集路由键
                .to(behaviorExchange)//绑定到用户行为采集交换机
                .with(BEHAVIOR_ROUTING_KEY);//用户行为采集路由键
    }

    /**
     * 消息体使用 JSON 序列化（替代默认 JDK 序列化），跨语言友好、便于排查
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();//JSON消息转换器
    }
}
