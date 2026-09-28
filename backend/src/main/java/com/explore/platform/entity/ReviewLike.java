package com.explore.platform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 点评点赞实体（对应 tb_review_like）。
 * <p>
 * 表使用复合主键 (user_id, review_id)：
 * MyBatis-Plus 中复合主键不使用 @TableId 自增策略，插入时显式带上两个字段。
 *
 * @author explore-platform
 */
@Data
@TableName("tb_review_like")
public class ReviewLike implements Serializable {

    /**
     * 点赞用户ID（复合主键之一）
     */
    private Long userId;

    /**
     * 被赞点评ID（复合主键之一）
     */
    private Long reviewId;

    private LocalDateTime createTime;
}
