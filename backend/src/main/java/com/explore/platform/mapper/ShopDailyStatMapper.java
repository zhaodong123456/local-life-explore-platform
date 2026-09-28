package com.explore.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.explore.platform.entity.ShopDailyStat;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商铺日统计 Mapper
 * <p>
 * 预聚合采用 INSERT ... ON DUPLICATE KEY UPDATE（命中 uk_shop_date 唯一键），
 * 幂等可重跑：同一时间段重复聚合不会产生重复行，只会覆盖更新。
 *
 * @author explore-platform
 */
@Mapper
public interface ShopDailyStatMapper extends BaseMapper<ShopDailyStat> {

    /**
     * 预聚合当日访问量（浏览行为 behavior_type=1，按 create_time 天分组）
     */
    @Insert("INSERT INTO tb_shop_daily_stat (shop_id, stat_date, visit_count) " +
            "SELECT shop_id, DATE(create_time), COUNT(*) " +
            "FROM tb_user_behavior " +
            "WHERE behavior_type = 1 AND create_time >= #{start} AND create_time < #{end} " +
            "GROUP BY shop_id, DATE(create_time) " +
            "ON DUPLICATE KEY UPDATE visit_count = VALUES(visit_count)")
    int upsertVisitCount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 预聚合当日核销量（订单 status=3 已核销，按 use_time 天分组）
     */
    @Insert("INSERT INTO tb_shop_daily_stat (shop_id, stat_date, writeoff_count) " +
            "SELECT shop_id, DATE(use_time), COUNT(*) " +
            "FROM tb_voucher_order " +
            "WHERE status = 3 AND use_time >= #{start} AND use_time < #{end} " +
            "GROUP BY shop_id, DATE(use_time) " +
            "ON DUPLICATE KEY UPDATE writeoff_count = VALUES(writeoff_count)")
    int upsertWriteoffCount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 预聚合当日新增点评数与平均分（按 create_time 天分组；无点评的商铺不会产生行）
     */
    @Insert("INSERT INTO tb_shop_daily_stat (shop_id, stat_date, review_count, avg_score) " +
            "SELECT shop_id, DATE(create_time), COUNT(*), ROUND(AVG(score), 2) " +
            "FROM tb_review " +
            "WHERE create_time >= #{start} AND create_time < #{end} " +
            "GROUP BY shop_id, DATE(create_time) " +
            "ON DUPLICATE KEY UPDATE review_count = VALUES(review_count), avg_score = VALUES(avg_score)")
    int upsertReviewStat(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 查询商铺指定日期范围（含边界）的日统计，按日期升序
     */
    @Select("SELECT * FROM tb_shop_daily_stat " +
            "WHERE shop_id = #{shopId} AND stat_date >= #{startDate} AND stat_date <= #{endDate} " +
            "ORDER BY stat_date ASC")
    List<ShopDailyStat> selectRange(@Param("shopId") Long shopId,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);
}
