package com.explore.platform.task;

import com.explore.platform.mapper.ShopDailyStatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商铺日统计预聚合定时任务
 * <p>
 * 设计要点（面试讲解口径）：
 * - 商家看板禁止实时聚合大表（tb_user_behavior/tb_voucher_order/tb_review 数据量随时间增长），
 *   由本任务预聚合到 tb_shop_daily_stat，接口只查统计小表；
 * - 每小时第 5 分钟执行，重算 [昨日0点, 明日0点) 范围：既持续刷新"今日"数据，
 *   也自动补算"昨日"（应对跨天边界与服务停机漏算）；
 * - 聚合 SQL 用 INSERT ... ON DUPLICATE KEY UPDATE 命中 uk_shop_date 唯一键，
 *   幂等可重跑，重复执行不产生脏数据。
 *
 * @author explore-platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardStatTask {

    private final ShopDailyStatMapper shopDailyStatMapper;

    /**
     * 每小时第 5 分钟执行一次
     */
    @Scheduled(cron = "0 5 * * * ?")
    public void aggregateHourly() {
        aggregateYesterdayAndToday();
    }

    /**
     * 启动 10 秒后先跑一次，避免服务重启后统计断档
     */
    @Scheduled(initialDelay = 10_000, fixedDelay = Long.MAX_VALUE)
    public void aggregateOnStartup() {
        aggregateYesterdayAndToday();
    }

    /**
     * 聚合 [昨日0点, 明日0点)：覆盖昨日 + 今日两天数据
     */
    private void aggregateYesterdayAndToday() {
        LocalDateTime start = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        try {
            int n1 = shopDailyStatMapper.upsertVisitCount(start, end);
            int n2 = shopDailyStatMapper.upsertWriteoffCount(start, end);
            int n3 = shopDailyStatMapper.upsertReviewStat(start, end);
            log.info("商铺日统计聚合完成：访问量 {} 行、核销量 {} 行、点评 {} 行", n1, n2, n3);
        } catch (Exception e) {
            log.error("商铺日统计聚合失败", e);
        }
    }
}
