package com.explore.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券发布 / 编辑请求
 *
 * @author explore-platform
 */
@Data
@Schema(description = "优惠券发布/编辑请求")
public class VoucherRequest implements Serializable {

    @Schema(description = "所属商铺ID")
    @NotNull(message = "所属商铺不能为空")
    private Long shopId;

    @Schema(description = "券标题")
    @NotBlank(message = "券标题不能为空")
    @Size(max = 128, message = "标题过长")
    private String title;

    @Schema(description = "副标题")
    @Size(max = 255)
    private String subTitle;

    @Schema(description = "使用规则")
    @Size(max = 512)
    private String rules;

    @Schema(description = "支付金额")
    @NotNull(message = "支付金额不能为空")
    @Positive(message = "支付金额必须大于0")
    private BigDecimal payValue;

    @Schema(description = "抵扣金额")
    @NotNull(message = "抵扣金额不能为空")
    @Positive(message = "抵扣金额必须大于0")
    private BigDecimal actualValue;

    @Schema(description = "类型 0普通券 1秒杀券")
    @NotNull(message = "券类型不能为空")
    private Integer type;

    @Schema(description = "生效时间")
    private LocalDateTime beginTime;

    @Schema(description = "失效时间")
    private LocalDateTime endTime;

    // ============ 秒杀券专属字段（type=1 时必填） ============

    @Schema(description = "秒杀库存（秒杀券必填）")
    @Positive(message = "库存必须大于0")
    private Integer stock;

    @Schema(description = "秒杀开始时间")
    private LocalDateTime seckillBegin;

    @Schema(description = "秒杀结束时间")
    private LocalDateTime seckillEnd;
}
