package com.explore.platform.ratelimit;

import com.explore.platform.config.SeckillRateLimitProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 秒杀接口本地限流器（两级限流中的"接口级"，单机维度）。
 * <p>
 * 【两级限流设计】
 * 1. 网关层（一级）：Nginx limit_req / 网关令牌桶，按 IP+接口 做集群入口粗粒度限流，
 *    拦截明显恶意流量，设计见《阶段0-需求设计文档》7 系统架构图，本地单机部署不重复实现；
 * 2. 接口层（二级，本类）：单机固定窗口计数 —— 全局 QPS 兜底 + 单用户窗口限次。
 *    用 Caffeine 存储单用户计数器，过期自动清理，无内存泄漏。
 * <p>
 * 注意：本地限流是单机口径，集群部署时总量 ≈ 单机阈值 × 实例数；
 * 严格的全局限流需用 Redis Lua 计数（阶段4 可平滑升级，接口不变）。
 *
 * @author explore-platform
 */
@Component
public class SeckillRateLimiter {

    private final SeckillRateLimitProperties props;

    /**
     * 单用户计数器：key=userId，value=窗口内已放行次数，窗口结束自动过期
     */
    private final Cache<Long, AtomicInteger> userCounters;

    /**
     * 全局计数器：当前秒窗口起点 + 窗口内计数
     */
    private final AtomicInteger globalCount = new AtomicInteger(0);

    private volatile long globalWindowSecond = -1;// 当前秒窗口起点
    //volatile 确保多线程可见性

    public SeckillRateLimiter(SeckillRateLimitProperties props) {
        this.props = props;
        // 初始化单用户计数器缓存
        this.userCounters = Caffeine.newBuilder()//Caffeine 缓存，用于存储单用户计数器，过期自动清理，无内存泄漏
                .expireAfterWrite(props.getPerUserWindowSeconds(), TimeUnit.SECONDS)
                .maximumSize(100_000)// 最大缓存大小，防止内存泄漏
                .build();// 构建缓存实例
    }

    /**
     * 尝试放行一次秒杀请求
     *
     * @return true 放行；false 触发限流
     */
    public boolean tryAcquire(Long userId) {
        return tryGlobal() && tryPerUser(userId);
    }

    /**
     * 全局固定窗口限流：每秒重置计数
     */
    private boolean tryGlobal() {
        long nowSecond = System.currentTimeMillis() / 1000;
        if (nowSecond != globalWindowSecond) {
            synchronized (this) {//this 是当前类实例，确保线程安全，避免竞态条件
                //synchronized (this) 确保在当前线程内执行，避免竞态条件
                if (nowSecond != globalWindowSecond) {
                    globalWindowSecond = nowSecond;
                    globalCount.set(0);
                }
            }
        }
        return globalCount.incrementAndGet() <= props.getGlobalQps();
    }

    /**
     * 单用户固定窗口限流：窗口内超过次数即拒绝（防同账号脚本刷单）
     */
    private boolean tryPerUser(Long userId) {
        // Caffeine 的 get(key, loader) 对同一 key 的加载是原子的
        AtomicInteger counter = userCounters.get(userId, k -> new AtomicInteger(0));
        if (counter == null) {// 如果用户不存在计数器，直接放行
            return true;
        }
        synchronized (counter) {
            return counter.incrementAndGet() <= props.getPerUserCount();
        }
    }
}
