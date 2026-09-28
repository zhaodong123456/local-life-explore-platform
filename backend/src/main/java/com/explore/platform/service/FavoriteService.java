package com.explore.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.vo.FavoriteVO;

/**
 * 商铺收藏服务
 *
 * @author explore-platform
 */
public interface FavoriteService {

    /**
     * 收藏商铺（同一用户对同一商铺只能收藏一次）
     */
    void favorite(Long userId, Long shopId);

    /**
     * 取消收藏（幂等）
     */
    void unfavorite(Long userId, Long shopId);

    /**
     * 我的收藏分页（按收藏时间倒序，携带商铺摘要）
     */
    IPage<FavoriteVO> pageMyFavorites(Long userId, Integer pageNum, Integer pageSize);
}
