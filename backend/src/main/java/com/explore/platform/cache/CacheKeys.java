package com.explore.platform.cache;

/**
 * 缓存 Key / 频道常量统一定义，避免散落在代码中
 *
 * @author explore-platform
 */
public final class CacheKeys {

    private CacheKeys() {
    }

    /**
     * 商铺 Redis Key 前缀：cache:shop:{shopId}
     */
    public static final String SHOP_PREFIX = "cache:shop:";

    /**
     * Redis 中空值的标记内容（防穿透：DB 确认不存在时写入）
     */
    public static final String NULL_HOLDER = "#NULL#";

    /**
     * 回源分布式锁前缀：lock:shop:{shopId}
     */
    public static final String SHOP_LOCK_PREFIX = "lock:shop:";

    /**
     * 商铺ID 布隆过滤器 Key
     */
    public static final String SHOP_BLOOM = "bloom:shop:ids";

    /**
     * 商铺缓存失效广播频道（多实例下删除各实例的 Caffeine 本地缓存）
     */
    public static final String SHOP_EVICT_CHANNEL = "channel:cache:shop:evict";

    /**
     * 秒杀库存 Redis Key 前缀（库存预热到 Redis，Lua 原子扣减）：seckill:stock:{voucherId}
     */
    public static final String SECKILL_STOCK_PREFIX = "seckill:stock:";

    /**
     * 秒杀下单用户/订单标记集合前缀（一人一单 + 消费端对账）：seckill:order:set:{voucherId}
     */
    public static final String SECKILL_ORDER_SET_PREFIX = "seckill:order:set:";

    /**
     * 秒杀 DB 兜底扣减的分布式锁前缀：lock:seckill:{voucherId}
     */
    public static final String SECKILL_LOCK_PREFIX = "lock:seckill:";

    /**
     * 个性化推荐列表 Key 前缀（ZSet：member=shopId, score=预测兴趣分）：recommend:user:{userId}
     */
    public static final String RECOMMEND_USER_PREFIX = "recommend:user:";

    /**
     * 热门商铺兜底榜单 Key（ZSet：member=shopId, score=热度分）：recommend:hot
     * <p>
     * 冷启动策略：新用户无行为 / 推荐结果为空时返回该榜单
     */
    public static final String RECOMMEND_HOT_KEY = "recommend:hot";

    public static String shopKey(Long shopId) {
        return SHOP_PREFIX + shopId;
    }

    public static String shopLock(Long shopId) {
        return SHOP_LOCK_PREFIX + shopId;
    }

    public static String seckillStockKey(Long voucherId) {
        return SECKILL_STOCK_PREFIX + voucherId;
    }

    public static String seckillOrderSetKey(Long voucherId) {
        return SECKILL_ORDER_SET_PREFIX + voucherId;
    }

    public static String seckillLock(Long voucherId) {
        return SECKILL_LOCK_PREFIX + voucherId;
    }

    public static String recommendUserKey(Long userId) {
        return RECOMMEND_USER_PREFIX + userId;
    }
}
