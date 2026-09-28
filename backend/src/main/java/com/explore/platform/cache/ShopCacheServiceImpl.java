package com.explore.platform.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.explore.platform.config.ShopCacheProperties;
import com.explore.platform.entity.Shop;
import com.explore.platform.mapper.ShopMapper;
import com.explore.platform.vo.ShopVO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 商铺多级缓存服务实现。
 * <p>
 * 查询链路：Caffeine 一级缓存 → Redis 二级缓存 → 分布式锁 + DB 回源。
 * <p>
 * 【与黑马点评的差异化设计】
 * 1. 黑马只用 Redis 单级缓存，本项目增加 Caffeine 本地缓存，绝大多数热点请求不经过网络；
 * 2. 防穿透除空值缓存外，增加 Redisson 分布式布隆过滤器（集群共享、重启不丢）；
 * 3. 防击穿使用"本地分段锁 + Redisson 分布式锁"两层，减少 Redis 锁竞争；
 * 4. TTL 全部基础值 + 随机抖动，且两级缓存 TTL 错开，防雪崩；
 * 5. Cache-Aside 更新时通过 Redis Pub/Sub 广播失效，解决多实例下 Caffeine 本地缓存一致性。
 *
 * @author explore-platform
 */
@Slf4j
@Service
public class ShopCacheServiceImpl implements ShopCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final RedissonClient redissonClient;

    private final ShopMapper shopMapper;

    private final ShopCacheProperties props;

    /**
     * L1 Caffeine 缓存
     */
    private final Cache<Long, CacheEntry> caffeine;

    /**
     * 分布式布隆过滤器（Redis 中，多实例共享）
     */
    private final RBloomFilter<Long> bloom;

    /**
     * 固定 256 段本地锁（按 shopId 哈希），无内存泄漏、无需动态移除
     */
    private final Object[] stripedLocks;

    /**
     * 布隆过滤器是否完成初始化/预热（冷启动期间不启用，避免误判合法请求）
     */
    private volatile boolean bloomReady = false;

    public ShopCacheServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                RedissonClient redissonClient,
                                ShopMapper shopMapper,
                                ShopCacheProperties props) {
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.shopMapper = shopMapper;
        this.props = props;
        this.bloom = redissonClient.getBloomFilter(CacheKeys.SHOP_BLOOM);

        // Caffeine 自定义 Expiry：每个条目在创建时算好 TTL（含随机抖动），空值条目短 TTL
        this.caffeine = Caffeine.newBuilder()
                .maximumSize(props.getCaffeineMaxSize())
                .expireAfter(new Expiry<Long, CacheEntry>() {
                    @Override
                    public long expireAfterCreate(Long key, CacheEntry value, long currentTime) {
                        return value.ttlNanos();
                    }

                    @Override
                    public long expireAfterUpdate(Long key, CacheEntry value, long currentTime,
                                                  long currentDuration) {
                        return value.ttlNanos();
                    }

                    @Override
                    public long expireAfterRead(Long key, CacheEntry value, long currentTime,
                                                long currentDuration) {
                        // 读不改变存活时间
                        return currentDuration;
                    }
                })
                .build();

        this.stripedLocks = new Object[256];
        for (int i = 0; i < stripedLocks.length; i++) {
            stripedLocks[i] = new Object();
        }
    }

    @Override
    public ShopVO getById(Long id) {
        if (id == null || id <= 0) {
            return null;
        }
        // 缓存总开关关闭时退化为直接查库
        if (!props.isEnabled()) {
            Shop shop = shopMapper.selectById(id);
            return shop == null ? null : toVO(shop);
        }

        // ---------- L1：Caffeine ----------
        CacheEntry entry = caffeine.getIfPresent(id);
        if (entry != null) {
            return entry.empty() ? null : entry.shop();
        }

        // ---------- 本地分段锁：同 JVM 内只放一个线程去查 Redis / 抢分布式锁 ----------
        Object localLock = stripedLocks[(int) (id & 0x7fffffff) % stripedLocks.length];
        synchronized (localLock) {
            // 双重检查 L1（等锁期间可能已被其他线程回填）
            entry = caffeine.getIfPresent(id);
            if (entry != null) {
                return entry.empty() ? null : entry.shop();
            }

            // ---------- L2：Redis ----------
            RedisRead read = readFromRedis(id);
            if (read.hit) {
                if (read.cachedNull) {
                    // 命中空值标记：回填 L1 短 TTL
                    putCaffeineEmpty(id);
                    return null;
                }
                caffeine.put(id, CacheEntry.of(read.shop, caffeineTtlNanos(false)));
                return read.shop;
            }

            // ---------- 布隆过滤器：L2 未命中时快速拒绝明显不存在的ID（防穿透） ----------
            if (bloomReady && props.isBloomEnabled() && !bloom.contains(id)) {
                log.debug("布隆过滤器判定商铺不存在，直接拒绝：shopId={}", id);
                // 双保险写空值缓存，防止布隆误判窗口/过滤器被重建期间穿透
                writeNullCaches(id);
                return null;
            }

            // ---------- Redisson 分布式锁：集群范围内单点回源（防击穿） ----------
            RLock rLock = redissonClient.getLock(CacheKeys.shopLock(id));
            boolean locked = false;
            try {
                locked = rLock.tryLock(
                        props.getLockWaitTime().toMillis(),
                        props.getLockLeaseTime().toMillis(),
                        TimeUnit.MILLISECONDS);
                if (locked) {
                    // 双重检查 L1 / L2：等锁期间持锁线程可能已完成回填
                    entry = caffeine.getIfPresent(id);
                    if (entry != null) {
                        return entry.empty() ? null : entry.shop();
                    }
                    RedisRead reread = readFromRedis(id);
                    if (reread.hit) {
                        if (reread.cachedNull) {
                            putCaffeineEmpty(id);
                            return null;
                        }
                        caffeine.put(id, CacheEntry.of(reread.shop, caffeineTtlNanos(false)));
                        return reread.shop;
                    }

                    // 真正回源 DB
                    Shop shop = shopMapper.selectById(id);
                    if (shop == null) {
                        // 空值双级缓存（防穿透核心手段）
                        writeNullCaches(id);
                        return null;
                    }
                    ShopVO vo = toVO(shop);
                    writeRedis(id, vo);
                    caffeine.put(id, CacheEntry.of(vo, caffeineTtlNanos(false)));
                    return vo;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("回源加锁被中断", e);
            } finally {
                if (locked && rLock.isHeldByCurrentThread()) {
                    rLock.unlock();
                }
            }

            // 未抢到分布式锁：预期持锁线程已回填，短暂等待后读缓存
            return readAfterLockLost(id);
        }
    }

    @Override
    public void evict(Long id) {
        if (id == null) {
            return;
        }
        // 1. 删 Redis 二级缓存（失败靠短 TTL 兜底，最终一致）
        try {
            redisTemplate.delete(CacheKeys.shopKey(id));
        } catch (Exception e) {
            log.warn("删除 Redis 商铺缓存失败，等待TTL兜底：shopId={}", id, e);
        }
        // 2. 删本实例 Caffeine
        caffeine.invalidate(id);
        // 3. 广播给其他实例删它们的本地 Caffeine
        try {
            redisTemplate.convertAndSend(CacheKeys.SHOP_EVICT_CHANNEL, id);
        } catch (Exception e) {
            log.warn("广播商铺缓存失效消息失败：shopId={}", id, e);
        }
    }

    @Override
    public void invalidateLocal(Long id) {
        if (id != null) {
            caffeine.invalidate(id);
            log.debug("收到失效广播，已删除本地 Caffeine：shopId={}", id);
        }
    }

    @Override
    public void warmUp() {
        if (!props.isEnabled() || !props.isPrewarmEnabled()) {
            return;
        }
        long start = System.currentTimeMillis();
        try {
            // 1. 布隆过滤器：tryInit 已存在时返回 false（多实例共享，不重复初始化）
            if (props.isBloomEnabled()) {
                bloom.tryInit(props.getBloomExpectedInsertions(), props.getBloomFalseProbability());
            }

            // 2. 全量商铺ID加入布隆过滤器
            List<Shop> all = shopMapper.selectList(new LambdaQueryWrapper<Shop>()
                    .select(Shop::getId));
            if (props.isBloomEnabled()) {
                all.forEach(s -> bloom.add(s.getId()));
            }

            // 3. 热点商铺预热到两级缓存：营业中、按创建时间倒序前 N 家
            List<Shop> hot = shopMapper.selectList(new LambdaQueryWrapper<Shop>()
                    .eq(Shop::getStatus, 1)
                    .orderByDesc(Shop::getCreateTime)
                    .last("LIMIT " + props.getPrewarmCount()));
            for (Shop shop : hot) {
                ShopVO vo = toVO(shop);
                writeRedis(shop.getId(), vo);
                caffeine.put(shop.getId(), CacheEntry.of(vo, caffeineTtlNanos(false)));
            }

            // 布隆就绪标记放在最后：预热期间的请求走正常回源逻辑，不会被误杀
            bloomReady = props.isBloomEnabled();
            log.info("商铺缓存预热完成：布隆 {} 个ID，预热热点 {} 家，耗时 {} ms",
                    all.size(), hot.size(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            // 预热失败不影响应用启动：布隆未就绪时查询走正常回源
            log.warn("商铺缓存预热失败，本次运行退化为按需加载", e);
        }
    }

    /**
     * 未抢到分布式锁的兜底：短暂等待 → 读缓存 → 仍无则直接查库（避免持锁线程异常导致饿死）
     */
    private ShopVO readAfterLockLost(Long id) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        CacheEntry entry = caffeine.getIfPresent(id);
        if (entry != null) {
            return entry.empty() ? null : entry.shop();
        }
        RedisRead read = readFromRedis(id);
        if (read.hit) {
            return read.cachedNull ? null : read.shop;
        }
        // 极端兜底：直接查库一次（查到也回填，加速后续请求）
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            return null;
        }
        ShopVO vo = toVO(shop);
        try {
            writeRedis(id, vo);
            caffeine.put(id, CacheEntry.of(vo, caffeineTtlNanos(false)));
        } catch (Exception e) {
            log.warn("兜底回填缓存失败：shopId={}", id, e);
        }
        return vo;
    }

    /**
     * 空值写入两级缓存（防穿透）
     */
    private void writeNullCaches(Long id) {
        long ttlMs = randomTtlMs(props.getRedisNullTtl(), props.getRedisNullJitter());
        redisTemplate.opsForValue().set(CacheKeys.shopKey(id), CacheKeys.NULL_HOLDER,
                Duration.ofMillis(ttlMs));
        putCaffeineEmpty(id);
    }

    private void putCaffeineEmpty(Long id) {
        long ttlNanos = Duration.ofMillis(
                randomTtlMs(props.getCaffeineNullTtl(), props.getCaffeineNullJitter())).toNanos();
        caffeine.put(id, CacheEntry.empty(ttlNanos));
    }

    private void writeRedis(Long id, ShopVO vo) {
        long ttlMs = randomTtlMs(props.getRedisTtl(), props.getRedisJitter());
        redisTemplate.opsForValue().set(CacheKeys.shopKey(id), vo, Duration.ofMillis(ttlMs));
    }

    private long caffeineTtlNanos(boolean empty) {
        long ttlMs = empty
                ? randomTtlMs(props.getCaffeineNullTtl(), props.getCaffeineNullJitter())
                : randomTtlMs(props.getCaffeineTtl(), props.getCaffeineJitter());
        return Duration.ofMillis(ttlMs).toNanos();
    }

    /**
     * 基础 TTL + [0, jitter) 随机抖动。大量 key 同时过期时被打散到时间窗口内（防雪崩）
     */
    private long randomTtlMs(Duration base, Duration jitter) {
        long jitterMs = jitter.toMillis();
        long extra = jitterMs <= 0 ? 0 : ThreadLocalRandom.current().nextLong(jitterMs);
        return base.toMillis() + extra;
    }

    /**
     * 从 Redis 读取并区分三种状态：未命中 / 空值标记 / 正常商铺
     */
    private RedisRead readFromRedis(Long id) {
        Object value = redisTemplate.opsForValue().get(CacheKeys.shopKey(id));
        if (value == null) {
            return RedisRead.MISS;
        }
        if (CacheKeys.NULL_HOLDER.equals(value)) {
            return RedisRead.NULL;
        }
        return new RedisRead(true, false, (ShopVO) value);
    }

    private ShopVO toVO(Shop shop) {
        ShopVO vo = new ShopVO();
        BeanUtils.copyProperties(shop, vo);
        return vo;
    }

    /**
     * Redis 读取结果内部载体
     */
    private static final class RedisRead {

        static final RedisRead MISS = new RedisRead(false, false, null);

        static final RedisRead NULL = new RedisRead(true, true, null);

        final boolean hit;

        final boolean cachedNull;

        final ShopVO shop;

        RedisRead(boolean hit, boolean cachedNull, ShopVO shop) {
            this.hit = hit;
            this.cachedNull = cachedNull;
            this.shop = shop;
        }
    }
}
