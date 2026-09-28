package com.explore.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内消息实体（对应 tb_message）
 * <p>
 * 消息类型：1点赞提醒 2优惠券到期 3入驻审核结果 4点评审核结果
 *
 * @author explore-platform
 */
@Data
@TableName("tb_message")
public class Message implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long receiverUserId;

    /**
     * 类型 1点赞提醒 2优惠券到期 3入驻审核结果 4点评审核结果
     */
    private Integer type;

    private String title;

    private String content;

    private String bizType;

    private Long bizId;

    /**
     * 0未读 1已读
     */
    private Integer isRead;

    private LocalDateTime createTime;

    private LocalDateTime readTime;
}
