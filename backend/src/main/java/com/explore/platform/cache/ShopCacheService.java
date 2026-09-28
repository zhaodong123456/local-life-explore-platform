package com.explore.platform.cache;

import com.explore.platform.vo.ShopVO;

/**
 * 商铺多级缓存服务
 *
 * @author explore-platform
 */
public interface ShopCacheService {

    /**
     * 按ID查询商铺：Caffeine 一级 → Redis 二级 → 分布式锁回源 DB。
     *
     * @param id 商铺ID
     * @return 商铺信息；不存在返回 null（调用方决定如何提示）
     */
    ShopVO getById(Long id);

    /**
     * 失效缓存：删 Redis + 删本地 Caffeine + 广播其他实例删本地。
     * 在商铺写操作（编辑/上下架/审核）成功后调用。
     */
    void evict(Long id);

    /**
     * 缓存预热：加载热点商铺到两级缓存，并（重建）布隆过滤器。
     * 应用启动就绪后自动执行，也可手动触发。
     */
    void warmUp();

    /**
     * 仅失效本实例 Caffeine 本地缓存。
     * 供 Redis Pub/Sub 失效消息监听调用（其他实例 evict 后广播到本实例）。
     */
    void invalidateLocal(Long id);
}
