package com.explore.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 商家入驻申请请求
 *
 * @author explore-platform
 */
@Data
@Schema(description = "商家入驻申请请求")
public class MerchantApplyRequest implements Serializable {

    @Schema(description = "商家/公司名称")
    @NotBlank(message = "商家名称不能为空")
    @Size(max = 128, message = "商家名称过长")
    private String merchantName;

    @Schema(description = "营业执照编号")
    @NotBlank(message = "营业执照编号不能为空")
    @Size(max = 64, message = "营业执照编号过长")
    private String licenseNo;

    @Schema(description = "营业执照照片URL")
    @Size(max = 255, message = "照片地址过长")
    private String licenseImg;

    @Schema(description = "联系人")
    @NotBlank(message = "联系人不能为空")
    @Size(max = 32, message = "联系人名称过长")
    private String contactName;

    @Schema(description = "联系电话")
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;
}
