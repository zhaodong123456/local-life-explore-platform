package com.explore.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 我的收藏展示对象（收藏记录 + 商铺摘要信息）
 *
 * @author explore-platform
 */
@Data
@Schema(description = "收藏信息")
public class FavoriteVO implements Serializable {

    @Schema(description = "商铺ID")
    private Long shopId;

    @Schema(description = "收藏时间")
    private LocalDateTime favoriteTime;

    @Schema(description = "商铺名称")
    private String name;

    @Schema(description = "类型ID")
    private Integer typeId;

    @Schema(description = "类型名称")
    private String typeName;

    @Schema(description = "所在城市")
    private String city;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "封面图")
    private String coverImg;

    @Schema(description = "人均消费")
    private BigDecimal avgPrice;

    @Schema(description = "商铺状态 0待审核 1营业 2已下架")
    private Integer status;
}
