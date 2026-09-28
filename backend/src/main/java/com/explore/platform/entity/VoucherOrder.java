package com.explore.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券订单实体（对应 tb_voucher_order）
 * <p>
 * 注意：id 为雪花算法生成（IdType.ASSIGN_ID），表上有 uk_user_voucher(user_id, voucher_id)
 * 唯一键，作为"一人一单"与订单幂等的数据库层兜底。
 *
 * @author explore-platform
 */
@Data
@TableName("tb_voucher_order")
public class VoucherOrder implements Serializable {

    /**
     * 订单ID（雪花算法，秒杀接口内先生成再随 MQ 消息传递）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private Long voucherId;

    /**
     * 商铺ID（冗余，便于商家侧查询/核销）
     */
    private Long shopId;

    /**
     * 订单类型 0普通 1秒杀
     */
    private Integer orderType;

    /**
     * 1未支付 2已支付待核销 3已核销 4已退款 5已关闭
     */
    private Integer status;

    /**
     * 支付方式（模拟支付时写入）
     */
    private Integer payType;

    private LocalDateTime createTime;

    private LocalDateTime payTime;

    private LocalDateTime useTime;
}
