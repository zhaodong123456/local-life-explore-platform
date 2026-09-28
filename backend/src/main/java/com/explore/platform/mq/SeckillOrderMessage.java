package com.explore.platform.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 秒杀下单消息：秒杀接口预生成订单ID后投递，消费者据此异步创建订单。
 * <p>
 * 幂等设计：orderId 在接口侧生成并作为消息的业务唯一标识，
 * 消费端落库前以 (user_id, voucher_id) 唯一键兜底，重复投递不会产生重复订单。
 *
 * @author explore-platform
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderMessage implements Serializable {

    private Long orderId;

    private Long userId;

    private Long voucherId;
}
