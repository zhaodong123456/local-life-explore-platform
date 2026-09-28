package com.explore.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商铺日统计实体（对应 tb_shop_daily_stat）
 * <p>
 * 由定时任务预聚合写入，商家后台只查本表，不允许实时聚合大表。
 * uk_shop_date(shop_id, stat_date) 唯一键保证每个商铺每天一行，upsert 幂等可重跑。
 *
 * @author explore-platform
 */
@Data
@TableName("tb_shop_daily_stat")
public class ShopDailyStat implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shopId;

    /**
     * 统计日期（精确到天）
     */
    private LocalDate statDate;

    /**
     * 当日访问量（tb_user_behavior behavior_type=1 按天计数）
     */
    private Integer visitCount;

    /**
     * 当日核销量（tb_voucher_order status=3 按 use_time 天计数）
     */
    private Integer writeoffCount;

    /**
     * 当日新增点评数
     */
    private Integer reviewCount;

    /**
     * 当日新增点评平均分（1~5，无点评则为 NULL）
     */
    private BigDecimal avgScore;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
