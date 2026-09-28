package com.explore.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.dto.ReviewAuditRequest;
import com.explore.platform.dto.ReviewPublishRequest;
import com.explore.platform.vo.ReviewVO;

/**
 * 点评服务
 *
 * @author explore-platform
 */
public interface ReviewService {

    /**
     * 发布点评（初始待审核；同一用户对同一商铺只能有一条有效点评）
     *
     * @return 点评ID
     */
    Long publish(Long userId, Long shopId, ReviewPublishRequest request);

    /**
     * 商铺点评分页（公开，仅审核通过；登录用户带 liked 标记）
     */
    IPage<ReviewVO> pageShopReviews(Long shopId, Integer pageNum, Integer pageSize, Long currentUserId);

    /**
     * 我的点评分页（含待审核/驳回）
     */
    IPage<ReviewVO> pageMyReviews(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 管理员审核列表分页（可按状态过滤 0待审核 1已通过 2已驳回）
     */
    IPage<ReviewVO> pageAllReviews(Integer pageNum, Integer pageSize, Integer status);

    /**
     * 点赞（防重复）
     */
    void like(Long userId, Long reviewId);

    /**
     * 取消点赞（幂等）
     */
    void unlike(Long userId, Long reviewId);

    /**
     * 管理员审核点评
     */
    void audit(Long adminUserId, Long reviewId, ReviewAuditRequest request);
}
