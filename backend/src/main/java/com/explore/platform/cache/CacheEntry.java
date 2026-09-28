package com.explore.platform.cache;

import com.explore.platform.vo.ShopVO;

/**
 * Caffeine 一级缓存条目包装。
 * <p>
 * 为什么需要包装类而不是直接存 ShopVO：
 * 1. 需要表达"空值"（数据库确认不存在），用 empty=true 与未缓存区分开，用于防穿透；
 * 2. 缓存雪崩防护要求每条目 TTL 随机化，Caffeine 的自定义 Expiry 需要按条目取 TTL，
 *    因此在条目创建时就算好 ttlNanos 存进来。
 *
 * @param shop     商铺数据，empty=true 时为 null
 * @param empty    是否为空值标记
 * @param ttlNanos 本条目的存活时间（纳秒），已含随机抖动
 * @author explore-platform
 */
public record CacheEntry(ShopVO shop, boolean empty, long ttlNanos) {

    public static CacheEntry of(ShopVO shop, long ttlNanos) {
        return new CacheEntry(shop, false, ttlNanos);
    }

    public static CacheEntry empty(long ttlNanos) {
        return new CacheEntry(null, true, ttlNanos);
    }
}
