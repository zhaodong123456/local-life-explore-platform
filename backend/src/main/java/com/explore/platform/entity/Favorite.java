package com.explore.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商铺收藏实体（对应 tb_favorite）。
 * <p>
 * 表使用复合主键 (user_id, shop_id) 天然防重，
 * 与点赞表同理：不声明 @TableId 自增，插入时显式带两个字段。
 *
 * @author explore-platform
 */
@Data
@TableName("tb_favorite")
public class Favorite implements Serializable {

    /**
     * 收藏用户ID（复合主键之一）
     */
    private Long userId;

    /**
     * 被收藏商铺ID（复合主键之一）
     */
    private Long shopId;

    private LocalDateTime createTime;
}
