package com.explore.platform.task;

import com.explore.platform.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 推荐计算定时任务
 * <p>
 * 每小时执行一次：读取近 30 天行为数据 → 计算用户相似度 → 推荐列表写 Redis。
 * Redis TTL 2 小时，与调度周期错开，避免任务失败时推荐列表立即失效。
 *
 * @author explore-platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendTask {

    private final RecommendService recommendService;

    /**
     * 每小时执行一次（整点）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void refreshRecommendations() {
        try {
            log.info("定时任务：开始刷新推荐列表");
            recommendService.refreshRecommendations();
        } catch (Exception e) {
            log.error("定时任务：刷新推荐列表失败", e);
        }
    }

    /**
     * 启动 5 分钟后先跑一次，避免服务刚启动时 Redis 无推荐数据（冷启动等待）
     */
    @Scheduled(initialDelay = 5 * 60 * 1000, fixedDelay = Long.MAX_VALUE)
    public void refreshRecommendationsOnStartup() {
        refreshRecommendations();
    }
}
