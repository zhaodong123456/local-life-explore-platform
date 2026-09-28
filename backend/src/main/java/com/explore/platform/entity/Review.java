package com.explore.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 点评实体（对应 tb_review）
 *
 * @author explore-platform
 */
@Data
@TableName("tb_review")
public class Review implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shopId;

    private Long userId;

    private String title;

    private String content;

    private String images;

    /**
     * 评分 1~5
     */
    private Integer score;

    /**
     * 0待审核 1已通过 2已驳回
     */
    private Integer status;

    private Long auditAdminId;

    private LocalDateTime auditTime;

    private String auditRemark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
