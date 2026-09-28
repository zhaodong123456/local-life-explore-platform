package com.explore.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商铺展示对象
 *
 * @author explore-platform
 */
@Data
@Schema(description = "商铺信息")
public class ShopVO implements Serializable {

    @Schema(description = "商铺ID")
    private Long id;

    @Schema(description = "所属商家ID")
    private Long merchantId;

    @Schema(description = "商铺名称")
    private String name;

    @Schema(description = "类型ID")
    private Integer typeId;

    @Schema(description = "类型名称")
    private String typeName;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "封面图")
    private String coverImg;

    @Schema(description = "门店图片")
    private String images;

    @Schema(description = "简介")
    private String description;

    @Schema(description = "人均消费")
    private BigDecimal avgPrice;

    @Schema(description = "状态 0待审核 1营业 2已下架")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
