package com.explore.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户行为日志实体（对应 tb_user_behavior）
 * <p>
 * 行为类型：1浏览 2点赞 3收藏 4点评<br>
 * 行为权重：浏览1、点评3、点赞4、收藏5（权重越高表示用户兴趣越强）
 *
 * @author explore-platform
 */
@Data
@TableName("tb_user_behavior")
public class UserBehavior implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

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

    private LocalDateTime createTime;
}
