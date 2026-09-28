package com.explore.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券展示对象
 *
 * @author explore-platform
 */
@Data
@Schema(description = "优惠券信息")
public class VoucherVO implements Serializable {

    @Schema(description = "优惠券ID")
    private Long id;

    @Schema(description = "所属商铺ID")
    private Long shopId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "副标题")
    private String subTitle;

    @Schema(description = "使用规则")
    private String rules;

    @Schema(description = "支付金额")
    private BigDecimal payValue;

    @Schema(description = "抵扣金额")
    private BigDecimal actualValue;

    @Schema(description = "类型 0普通券 1秒杀券")
    private Integer type;

    @Schema(description = "状态 1上架 0下架")
    private Integer status;

    @Schema(description = "生效时间")
    private LocalDateTime beginTime;

    @Schema(description = "失效时间")
    private LocalDateTime endTime;

    @Schema(description = "秒杀剩余库存（仅秒杀券返回）")
    private Integer stock;

    @Schema(description = "所属商铺名称（商家券列表接口返回）")
    private String shopName;
}
