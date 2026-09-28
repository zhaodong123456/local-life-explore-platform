package com.explore.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.explore.platform.common.exception.BusinessException;
import com.explore.platform.dto.ReviewAuditRequest;
import com.explore.platform.dto.ReviewPublishRequest;
import com.explore.platform.entity.Review;
import com.explore.platform.entity.ReviewLike;
import com.explore.platform.entity.Shop;
import com.explore.platform.mapper.ReviewLikeMapper;
import com.explore.platform.mapper.ReviewMapper;
import com.explore.platform.mapper.ShopMapper;
import com.explore.platform.mq.BehaviorMessage;
import com.explore.platform.mq.BehaviorProducer;
import com.explore.platform.mq.NotifyMessage;
import com.explore.platform.mq.NotifyMessageProducer;
import com.explore.platform.service.ReviewService;
import com.explore.platform.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 点评服务实现
 *
 * @author explore-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;

    private final ReviewLikeMapper reviewLikeMapper;

    private final ShopMapper shopMapper;

    private final NotifyMessageProducer notifyMessageProducer;

    private final BehaviorProducer behaviorProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publish(Long userId, Long shopId, ReviewPublishRequest request) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException("商铺不存在");
        }

        // 一人一铺一评：存在待审核或已通过的点评则禁止重复发（已驳回的不占用资格）
        Long activeCount = reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getShopId, shopId)
                .eq(Review::getUserId, userId)
                .in(Review::getStatus, 0, 1));
        if (activeCount != null && activeCount > 0) {
            throw new BusinessException("您已点评过该商铺，请勿重复发布");
        }

        Review review = new Review();
        review.setShopId(shopId);
        review.setUserId(userId);
        BeanUtils.copyProperties(request, review);
        // 新点评默认待审核，由管理员审核后公开
        review.setStatus(0);
        reviewMapper.insert(review);
        // 阶段4-2：异步写用户行为（behavior_type=4 点评，权重3）用于协同过滤推荐
        try {
            behaviorProducer.send(new BehaviorMessage(userId, shopId, 4, 3));
        } catch (Exception e) {
            // 行为采集失败不影响点评发布主流程
            log.warn("点评行为采集失败：userId={}, shopId={}", userId, shopId, e);
        }
        return review.getId();
    }

    @Override
    public IPage<ReviewVO> pageShopReviews(Long shopId, Integer pageNum, Integer pageSize, Long currentUserId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException("商铺不存在");
        }
        return reviewMapper.selectShopReviewPage(new Page<>(pageNum, Math.min(pageSize, 100)), shopId, currentUserId);
    }

    @Override
    public IPage<ReviewVO> pageMyReviews(Long userId, Integer pageNum, Integer pageSize) {
        return reviewMapper.selectMyReviewPage(new Page<>(pageNum, Math.min(pageSize, 100)), userId);
    }

    @Override
    public IPage<ReviewVO> pageAllReviews(Integer pageNum, Integer pageSize, Integer status) {
        return reviewMapper.selectAllReviewPage(new Page<>(pageNum, Math.min(pageSize, 100)), status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void like(Long userId, Long reviewId) {
        Review review = requireVisibleReview(reviewId);

        // 应用层先查，给出友好提示；并发漏网时由复合主键兜底
        if (reviewLikeMapper.exists(userId, reviewId)) {
            throw new BusinessException("您已经点过赞了");
        }
        ReviewLike like = new ReviewLike();
        like.setUserId(userId);
        like.setReviewId(reviewId);
        try {
            reviewLikeMapper.insert(like);
        } catch (DuplicateKeyException e) {
            // 并发场景：复合主键 (user_id, review_id) 拒绝重复
            throw new BusinessException("您已经点过赞了");
        }
        // 阶段4-1：给点评作者发"点赞提醒"消息（type=1，排除自己赞自己）
        if (!review.getUserId().equals(userId)) {
            try {
                notifyMessageProducer.send(new NotifyMessage(
                        review.getUserId(),
                        1,
                        "您的点评被点赞",
                        "您的点评收到了一个赞，快去看看吧~",
                        "review_like",
                        reviewId));
            } catch (Exception e) {
                // 消息投递失败不影响点赞主流程（消息可靠性由 MQ 保证，这里仅记录）
                log.warn("点赞提醒消息投递失败：reviewId={}, liker={}", reviewId, userId, e);
            }
        }
        // 阶段4-2：异步写用户行为（behavior_type=2 点赞，权重4，挂在点评所属商铺上）用于协同过滤推荐
        try {
            behaviorProducer.send(new BehaviorMessage(userId, review.getShopId(), 2, 4));
        } catch (Exception e) {
            // 行为采集失败不影响点赞主流程
            log.warn("点赞行为采集失败：userId={}, shopId={}", userId, review.getShopId(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlike(Long userId, Long reviewId) {
        // 不校验点评是否仍存在/可见：删除是幂等操作，记录不存在影响行数为0
        reviewLikeMapper.deleteLike(userId, reviewId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long adminUserId, Long reviewId, ReviewAuditRequest request) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("点评不存在");
        }
        if (review.getStatus() != 0) {
            throw new BusinessException("该点评已审核，请勿重复操作");
        }

        review.setAuditAdminId(adminUserId);
        review.setAuditTime(LocalDateTime.now());
        review.setAuditRemark(request.getRemark());
        review.setStatus(Boolean.TRUE.equals(request.getApproved()) ? 1 : 2);
        reviewMapper.updateById(review);
        // 阶段4-1：给发布者发"点评审核结果"消息（type=4）
        try {
            notifyMessageProducer.send(new NotifyMessage(
                    review.getUserId(),
                    4,
                    review.getStatus() == 1 ? "点评审核通过" : "点评审核未通过",
                    review.getStatus() == 1
                            ? "您的点评已通过审核，现在对外可见"
                            : "您的点评未通过审核，原因：" + (request.getRemark() != null ? request.getRemark() : "不符合规范"),
                    "review_audit",
                    reviewId));
        } catch (Exception e) {
            log.warn("点评审核结果消息投递失败：reviewId={}", reviewId, e);
        }
    }

    /**
     * 点赞目标必须是已审核通过、对外可见的点评
     */
    private Review requireVisibleReview(Long reviewId) {
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("点评不存在");
        }
        if (review.getStatus() != 1) {
            throw new BusinessException("该点评尚未通过审核，无法点赞");
        }
        return review;
    }
}
