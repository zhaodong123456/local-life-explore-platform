package com.explore.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商家信息展示对象
 *
 * @author explore-platform
 */
@Data
@Schema(description = "商家信息")
public class MerchantVO implements Serializable {

    @Schema(description = "商家ID")
    private Long id;

    @Schema(description = "关联用户ID")
    private Long userId;

    @Schema(description = "商家名称")
    private String merchantName;

    @Schema(description = "营业执照编号")
    private String licenseNo;

    @Schema(description = "营业执照照片")
    private String licenseImg;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "状态 0待审核 1已通过 2已驳回")
    private Integer status;

    @Schema(description = "审核备注")
    private String auditRemark;

    @Schema(description = "申请时间")
    private LocalDateTime createTime;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;
}
