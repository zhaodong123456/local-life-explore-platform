package com.explore.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券实体（对应 tb_voucher）
 *
 * @author explore-platform
 */
@Data
@TableName("tb_voucher")
public class Voucher implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shopId;

    private String title;

    private String subTitle;

    private String rules;

    private BigDecimal payValue;//BigDecimal 优惠券支付金额，用于存储优惠券支付金额，避免精度损失

    private BigDecimal actualValue;

    /**
     * 0普通券 1秒杀券
     */
    private Integer type;

    /**
     * 1上架 0下架
     */
    private Integer status;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
