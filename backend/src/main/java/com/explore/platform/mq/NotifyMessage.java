package com.explore.platform.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 站内消息 MQ 消息体
 * <p>
 * 幂等键：bizType + bizId 唯一标识一个业务事件，
 * 消费端据此判断重复消息，不重复落库。
 *
 * @author explore-platform
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotifyMessage implements Serializable {

    /**
     * 接收人用户ID
     */
    private Long receiverUserId;

    /**
     * 消息类型 1点赞提醒 2优惠券到期 3入驻审核结果 4点评审核结果
     */
    private Integer type;

    private String title;

    private String content;

    /**
     * 业务类型标识（如 review_like / merchant_audit / review_audit）
     */
    private String bizType;

    /**
     * 关联业务ID（如 reviewId / merchantId）
     */
    private Long bizId;
}
