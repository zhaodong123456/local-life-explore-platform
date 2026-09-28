package com.explore.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商家信息实体（对应 tb_merchant）
 *
 * @author explore-platform
 */
@Data
@TableName("tb_merchant")
public class Merchant implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String merchantName;

    private String licenseNo;

    private String licenseImg;

    private String contactName;

    private String contactPhone;

    /**
     * 状态 0待审核 1已通过 2已驳回
     */
    private Integer status;

    private Long auditAdminId;

    private LocalDateTime auditTime;

    private String auditRemark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
