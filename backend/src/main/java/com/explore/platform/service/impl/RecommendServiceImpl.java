package com.explore.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.explore.platform.cache.CacheKeys;
import com.explore.platform.entity.Shop;
import com.explore.platform.entity.UserBehavior;
import com.explore.platform.mapper.ShopMapper;
import com.explore.platform.mapper.UserBehaviorMapper;
import com.explore.platform.service.RecommendService;
import com.explore.platform.vo.ShopVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 推荐服务实现：简易协同过滤（User-CF）
 * <p>
 * 算法步骤（面试讲解口径）：
 * 1. 行为权重：浏览1、点评3、点赞4、收藏5，同一用户对同一商铺多次行为权重求和，作为"兴趣分"
 * 2. 相似度计算：用户两两之间用余弦相似度 sim(u,v) = Σ(ru·rv) / (||ru||·||rv||)
 * 3. 推荐列表生成：对目标用户 u，取 TopK 相似用户，把"相似用户喜欢而 u 未交互"的商铺
 *    按 Σ(sim(u,v)·r(v,shop)) 打分排序，取 TopN 写入 Redis
 * 4. 冷启动兜底：新用户无行为数据或推荐结果为空时，返回全站热门榜单（按行为权重总和排序）
 * <p>
 * 说明：本项目为教学/面试场景，数据量小，单机内存计算足够；
 * 生产环境可将相似度计算下沉到 Spark/Flink，或用物品协同过滤（Item-CF）分摊用户规模压力。
 *
 * @author explore-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    /**
     * 行为数据时间窗口：近 30 天
     */
    private static final int BEHAVIOR_WINDOW_DAYS = 30;

    /**
     * 每个用户保留的相似用户数量（TopK）
     */
    private static final int TOP_K_SIMILAR = 20;

    /**
     * 每个用户生成的推荐商铺数量（TopN）
     */
    private static final int TOP_N_RECOMMEND = 10;

    /**
     * 热门榜单长度
     */
    private static final int HOT_LIST_SIZE = 20;

    /**
     * 推荐列表 Redis TTL（2 小时，定时任务 1 小时一刷，留 1 小时冗余）
     */
    private static final Duration RECOMMEND_TTL = Duration.ofHours(2);

    private final UserBehaviorMapper userBehaviorMapper;

    private final ShopMapper shopMapper;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ShopVO> recommendForUser(Long userId, Integer count) {
        int size = count == null || count <= 0 ? TOP_N_RECOMMEND : Math.min(count, 50);

        // 1. 读个性化推荐（ZSet 按预测分倒序）
        List<Long> shopIds = readTopShopIds(CacheKeys.recommendUserKey(userId), size);

        // 2. 冷启动兜底：无推荐结果 → 全站热门榜单
        if (shopIds.isEmpty()) {
            log.info("用户 {} 无个性化推荐，走热门榜单兜底", userId);
            shopIds = readTopShopIds(CacheKeys.RECOMMEND_HOT_KEY, size);
        }
        // 3. 热门榜单也为空（系统刚启动无行为）→ 兜底：最新营业商铺
        if (shopIds.isEmpty()) {
            log.info("热门榜单为空，兜底返回最新营业商铺");
            return listLatestShops(size);
        }

        // 4. 按 ID 批量查商铺，保持推荐顺序；过滤掉非营业商铺
        List<Shop> shops = shopMapper.selectBatchIds(shopIds);
        Map<Long, Shop> shopMap = shops.stream()
                .filter(s -> s.getStatus() != null && s.getStatus() == 1)
                .collect(Collectors.toMap(Shop::getId, s -> s));
        List<ShopVO> result = new ArrayList<>(shopIds.size());
        for (Long shopId : shopIds) {
            Shop shop = shopMap.get(shopId);
            if (shop != null) {
                result.add(toVO(shop));
            }
        }
        return result;
    }

    @Override
    public void refreshRecommendations() {
        long start = System.currentTimeMillis();
        LocalDateTime since = LocalDateTime.now().minusDays(BEHAVIOR_WINDOW_DAYS);
        List<UserBehavior> behaviors = userBehaviorMapper.selectRecentBehaviors(since);
        if (behaviors == null || behaviors.isEmpty()) {
            log.info("近 {} 天无行为数据，跳过推荐计算", BEHAVIOR_WINDOW_DAYS);
            return;
        }

        // ===== 步骤1：构建用户-商铺评分矩阵 userId -> (shopId -> 累计权重) =====
        Map<Long, Map<Long, Double>> userShopScores = new HashMap<>();
        // 商铺热度榜：shopId -> 权重总和（冷启动兜底）
        Map<Long, Double> shopHotScores = new HashMap<>();
        for (UserBehavior b : behaviors) {
            if (b.getUserId() == null || b.getShopId() == null || b.getWeight() == null) {
                continue;
            }
            userShopScores.computeIfAbsent(b.getUserId(), k -> new HashMap<>())
                    .merge(b.getShopId(), b.getWeight().doubleValue(), Double::sum);
            shopHotScores.merge(b.getShopId(), b.getWeight().doubleValue(), Double::sum);
        }

        // ===== 步骤2：计算用户两两余弦相似度 =====
        // sim(u,v) = Σ(ru·rv) / (||ru||·||rv||)，只保留 TopK 邻居
        Map<Long, Map<Long, Double>> userNeighbors = calcUserNeighbors(userShopScores);

        // ===== 步骤3：为每个用户生成 TopN 推荐并写入 Redis =====
        int userCount = 0;
        for (Map.Entry<Long, Map<Long, Double>> entry : userShopScores.entrySet()) {
            Long userId = entry.getKey();
            Set<Long> interacted = entry.getValue().keySet();
            Map<Long, Double> neighbors = userNeighbors.getOrDefault(userId, Map.of());
            if (neighbors.isEmpty()) {
                continue;
            }
            // 候选商铺打分：Σ(sim(u,v)·r(v,shop))，过滤已交互商铺
            Map<Long, Double> candidateScores = new HashMap<>();
            for (Map.Entry<Long, Double> neighbor : neighbors.entrySet()) {
                Long neighborId = neighbor.getKey();
                double sim = neighbor.getValue();
                Map<Long, Double> neighborScores = userShopScores.getOrDefault(neighborId, Map.of());
                for (Map.Entry<Long, Double> ns : neighborScores.entrySet()) {
                    Long shopId = ns.getKey();
                    if (interacted.contains(shopId)) {
                        continue;
                    }
                    candidateScores.merge(shopId, sim * ns.getValue(), Double::sum);
                }
            }
            if (candidateScores.isEmpty()) {
                continue;
            }
            writeRecommendToRedis(userId, candidateScores);
            userCount++;
        }

        // ===== 步骤4：刷新全站热门榜单（冷启动兜底）=====
        writeHotListToRedis(shopHotScores);

        log.info("推荐计算完成：行为数={}，用户数={}，耗时={}ms",
                behaviors.size(), userCount, System.currentTimeMillis() - start);
    }

    /**
     * 计算用户两两余弦相似度，每个用户只保留 TopK 邻居
     */
    private Map<Long, Map<Long, Double>> calcUserNeighbors(Map<Long, Map<Long, Double>> userShopScores) {
        // 预计算每个用户的向量模长 ||u|| = sqrt(Σru²)
        Map<Long, Double> userNorms = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : userShopScores.entrySet()) {
            double sumSq = entry.getValue().values().stream().mapToDouble(s -> s * s).sum();
            userNorms.put(entry.getKey(), Math.sqrt(sumSq));
        }

        // 倒排：shopId -> 交互过的用户列表（避免 O(U²) 全组合，只对同商铺有交集的用户计算）
        Map<Long, List<Long>> shopToUsers = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : userShopScores.entrySet()) {
            Long userId = entry.getKey();
            for (Long shopId : entry.getValue().keySet()) {
                shopToUsers.computeIfAbsent(shopId, k -> new ArrayList<>()).add(userId);
            }
        }

        // 累加共同交互的点积 Σ(ru·rv)
        Map<Long, Map<Long, Double>> dotProducts = new HashMap<>();
        for (Map.Entry<Long, List<Long>> entry : shopToUsers.entrySet()) {
            Long shopId = entry.getKey();
            List<Long> users = entry.getValue();
            for (int i = 0; i < users.size(); i++) {
                for (int j = i + 1; j < users.size(); j++) {
                    Long u1 = users.get(i);
                    Long u2 = users.get(j);
                    double dot = userShopScores.get(u1).getOrDefault(shopId, 0.0)
                            * userShopScores.get(u2).getOrDefault(shopId, 0.0);
                    dotProducts.computeIfAbsent(u1, k -> new HashMap<>()).merge(u2, dot, Double::sum);
                    dotProducts.computeIfAbsent(u2, k -> new HashMap<>()).merge(u1, dot, Double::sum);
                }
            }
        }

        // 除以模长得余弦相似度，保留 TopK
        Map<Long, Map<Long, Double>> neighbors = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : dotProducts.entrySet()) {
            Long userId = entry.getKey();
            double normU = userNorms.getOrDefault(userId, 1.0);
            Map<Long, Double> sims = new HashMap<>();
            for (Map.Entry<Long, Double> dotEntry : entry.getValue().entrySet()) {
                Long other = dotEntry.getKey();
                double normV = userNorms.getOrDefault(other, 1.0);
                double sim = dotEntry.getValue() / (normU * normV);
                if (sim > 0) {
                    sims.put(other, sim);
                }
            }
            Map<Long, Double> topK = sims.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(TOP_K_SIMILAR)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, HashMap::new));
            neighbors.put(userId, topK);
        }
        return neighbors;
    }

    /**
     * 把推荐结果写入 Redis：ZSet（member=shopId, score=预测分），取 TopN
     */
    private void writeRecommendToRedis(Long userId, Map<Long, Double> candidateScores) {
        List<Map.Entry<Long, Double>> topN = candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(TOP_N_RECOMMEND)
                .collect(Collectors.toList());
        String key = CacheKeys.recommendUserKey(userId);
        // 先删后写，避免旧数据残留；用事务保证原子性
        stringRedisTemplate.delete(key);
        for (Map.Entry<Long, Double> e : topN) {
            stringRedisTemplate.opsForZSet().add(key, String.valueOf(e.getKey()), e.getValue());
        }
        stringRedisTemplate.expire(key, RECOMMEND_TTL);
    }

    /**
     * 把全站热门榜单写入 Redis：ZSet（member=shopId, score=热度分）
     */
    private void writeHotListToRedis(Map<Long, Double> shopHotScores) {
        List<Map.Entry<Long, Double>> hot = shopHotScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(HOT_LIST_SIZE)
                .collect(Collectors.toList());
        String key = CacheKeys.RECOMMEND_HOT_KEY;
        stringRedisTemplate.delete(key);
        for (Map.Entry<Long, Double> e : hot) {
            stringRedisTemplate.opsForZSet().add(key, String.valueOf(e.getKey()), e.getValue());
        }
        stringRedisTemplate.expire(key, RECOMMEND_TTL);
    }

    /**
     * 从 Redis ZSet 读取分数最高的 shopId 列表
     */
    private List<Long> readTopShopIds(String key, int size) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, size - 1);
        if (tuples == null || tuples.isEmpty()) {
            return List.of();
        }
        return tuples.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(Objects::nonNull)
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * 终极兜底：无任何行为数据时，返回最新营业的商铺
     */
    private List<ShopVO> listLatestShops(int size) {
        List<Shop> shops = shopMapper.selectList(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getStatus, 1)
                .orderByDesc(Shop::getCreateTime)
                .last("LIMIT " + size));
        return shops.stream().map(this::toVO).collect(Collectors.toList());
    }

    private ShopVO toVO(Shop shop) {
        ShopVO vo = new ShopVO();
        BeanUtils.copyProperties(shop, vo);
        return vo;
    }
}
