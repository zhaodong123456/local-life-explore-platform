package com.explore.platform.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 秒杀券扩展实体（对应 tb_seckill_voucher）
 *
 * @author explore-platform
 */
@Data
@TableName("tb_seckill_voucher")
public class SeckillVoucher implements Serializable {

    @TableId
    private Long voucherId;

    private Integer initialStock;

    /**
     * DB 兜底库存（主库存阶段3放 Redis）
     */
    private Integer stock;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
