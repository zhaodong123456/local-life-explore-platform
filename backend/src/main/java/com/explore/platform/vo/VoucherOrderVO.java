package com.explore.platform.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券订单视图对象
 * <p>
 * id 用 String 返回：雪花 ID 超出 JS Number 安全整数范围（2^53），
 * 直接返回 Long 到前端会丢精度，统一序列化为字符串。
 *
 * @author explore-platform
 */
@Data
public class VoucherOrderVO implements Serializable {

    private String id;

    private Long voucherId;

    private Long shopId;

    /**
     * 冗余展示字段（避免前端二次查询）
     */
    private String voucherTitle;

    private String shopName;

    private BigDecimal payValue;

    /**
     * 订单类型 0普通 1秒杀
     */
    private Integer orderType;

    /**
     * 1未支付 2已支付待核销 3已核销 4已退款 5已关闭
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime payTime;

    private LocalDateTime useTime;
}
