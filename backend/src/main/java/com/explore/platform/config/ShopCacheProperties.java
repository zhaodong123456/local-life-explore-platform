package com.explore.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 商铺多级缓存配置（prefix: cache.shop）
 *
 * @author explore-platform
 */
@Data
@ConfigurationProperties(prefix = "cache.shop")
public class ShopCacheProperties {

    /**
     * 缓存总开关
     */
    private boolean enabled = true;

    /**
     * 是否开启缓存预热
     */
    private boolean prewarmEnabled = true;

    /**
     * 预热商铺数量（按创建时间倒序取营业中的前 N 家）
     */
    private int prewarmCount = 20;

    /**
     * 是否开启 Redisson 分布式布隆过滤器（防穿透第二道防线）
     */
    private boolean bloomEnabled = true;

    /**
     * 布隆过滤器预估容量
     */
    private long bloomExpectedInsertions = 10000L;

    /**
     * 布隆过滤器误判率
     */
    private double bloomFalseProbability = 0.01;

    /**
     * L1 Caffeine 正常条目基础 TTL
     */
    private Duration caffeineTtl = Duration.ofMinutes(5);

    /**
     * L1 Caffeine TTL 随机抖动上限（实际 TTL = 基础 + [0, 抖动)）
     */
    private Duration caffeineJitter = Duration.ofMinutes(2);

    /**
     * L1 Caffeine 空值条目 TTL（防穿透，故意设短）
     */
    private Duration caffeineNullTtl = Duration.ofSeconds(30);

    /**
     * L1 Caffeine 空值 TTL 抖动
     */
    private Duration caffeineNullJitter = Duration.ofSeconds(30);

    /**
     * L1 最大条目数
     */
    private long caffeineMaxSize = 10000L;

    /**
     * L2 Redis 正常条目基础 TTL
     */
    private Duration redisTtl = Duration.ofMinutes(30);

    /**
     * L2 Redis TTL 随机抖动上限
     */
    private Duration redisJitter = Duration.ofMinutes(10);

    /**
     * L2 Redis 空值条目 TTL
     */
    private Duration redisNullTtl = Duration.ofMinutes(2);

    /**
     * L2 Redis 空值 TTL 抖动
     */
    private Duration redisNullJitter = Duration.ofMinutes(1);

    /**
     * 回源分布式锁等待时间
     */
    private Duration lockWaitTime = Duration.ofSeconds(5);

    /**
     * 回源分布式锁持有时间（-1 启用看门狗自动续期时由 Redisson 处理，此处给兜底值）
     */
    private Duration lockLeaseTime = Duration.ofSeconds(30);
}
