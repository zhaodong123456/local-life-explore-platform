package com.explore.platform.service;

import com.explore.platform.vo.ShopVO;

import java.util.List;

/**
 * 推荐服务
 * <p>
 * 简易协同过滤（User-CF）推荐流：
 * 行为采集(MQ异步落库) → 定时任务计算用户相似度 → 推荐列表写Redis → 首页接口读取
 *
 * @author explore-platform
 */
public interface RecommendService {

    /**
     * 获取当前登录用户的个性化推荐商铺列表
     * <p>
     * 冷启动兜底：无行为数据或推荐结果为空时，返回全站热门榜单
     *
     * @param userId 用户ID
     * @param count  返回条数
     * @return 推荐商铺列表
     */
    List<ShopVO> recommendForUser(Long userId, Integer count);

    /**
     * 定时任务：基于近 N 天行为数据计算用户相似度，生成推荐列表写入 Redis
     * <p>
     * 算法步骤（面试讲解口径）：
     * 1. 构建用户-商铺评分矩阵：同一用户对同一商铺多次行为的权重求和
     * 2. 计算用户两两余弦相似度：sim(u,v) = Σ(ru·rv) / (||ru||·||rv||)
     * 3. 对每个用户取 TopK 相似用户，聚合他们交互过而目标用户未交互的商铺，
     *    按 Σ(sim(u,v)·r(v,shop)) 打分排序，得到 TopN 推荐列表
     * 4. 全站热门榜单（行为权重总和排序）作为冷启动兜底
     */
    void refreshRecommendations();
}
