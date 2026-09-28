package com.explore.platform.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户行为采集 MQ 消息体
 * <p>
 * 触发点：浏览商铺、点赞点评、收藏商铺、发布点评
 * 消费端：异步写入 tb_user_behavior，不阻塞主流程
 *
 * @author explore-platform
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorMessage implements Serializable {

    private Long userId;

    private Long shopId;

    /**
     * 行为类型：1浏览 2点赞 3收藏 4点评
     */
    private Integer behaviorType;

    /**
     * 行为权重：浏览1、点评3、点赞4、收藏5
     */
    private Integer weight;
}
