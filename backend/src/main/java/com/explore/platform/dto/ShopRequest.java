package com.explore.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商铺新建 / 编辑请求
 *
 * @author explore-platform
 */
@Data
@Schema(description = "商铺新建/编辑请求")
public class ShopRequest implements Serializable {

    @Schema(description = "商铺名称")
    @NotBlank(message = "商铺名称不能为空")
    @Size(max = 128, message = "商铺名称过长")
    private String name;

    @Schema(description = "商铺类型ID")
    @NotNull(message = "商铺类型不能为空")
    private Integer typeId;

    @Schema(description = "类型名称")
    private String typeName;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "详细地址")
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "地址过长")
    private String address;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "封面图URL")
    private String coverImg;

    @Schema(description = "门店图片，逗号分隔")
    private String images;

    @Schema(description = "简介")
    private String description;

    @Schema(description = "人均消费")
    private BigDecimal avgPrice;
}
