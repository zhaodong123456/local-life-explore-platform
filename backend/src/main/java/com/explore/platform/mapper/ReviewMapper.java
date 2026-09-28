package com.explore.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.explore.platform.entity.Review;
import com.explore.platform.vo.ReviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 点评 Mapper
 *
 * @author explore-platform
 */
@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    /**
     * 商铺公开点评分页（只查审核通过的）。
     * <p>
     * 关联 tb_user 带出昵称头像；
     * 相关子查询统计点赞数，以及"当前查看者是否已赞"（未登录时 userId 传 null，liked 恒为 0）。
     *
     * @param page   MP 分页参数（拦截器自动追加 COUNT 和 LIMIT）
     * @param shopId 商铺ID
     * @param userId 当前查看者ID（可空）
     */
    @Select("""
            SELECT r.id, r.shop_id AS shopId, r.user_id AS userId,
                   u.nickname, u.avatar,
                   r.title, r.content, r.images, r.score, r.status,
                   r.create_time AS createTime,
                   (SELECT COUNT(*) FROM tb_review_like l WHERE l.review_id = r.id) AS likeCount,
                   (SELECT COUNT(*) FROM tb_review_like l
                      WHERE l.review_id = r.id AND l.user_id = #{userId}) AS liked
            FROM tb_review r
            INNER JOIN tb_user u ON u.id = r.user_id
            WHERE r.shop_id = #{shopId} AND r.status = 1
            ORDER BY r.create_time DESC
            """)
    IPage<ReviewVO> selectShopReviewPage(Page<ReviewVO> page,
                                         @Param("shopId") Long shopId,
                                         @Param("userId") Long userId);

    /**
     * 我的点评分页（含待审核/已驳回，带审核备注）
     */
    @Select("""
            SELECT r.id, r.shop_id AS shopId, r.user_id AS userId,
                   u.nickname, u.avatar,
                   r.title, r.content, r.images, r.score, r.status,
                   r.audit_remark AS auditRemark,
                   r.create_time AS createTime,
                   (SELECT COUNT(*) FROM tb_review_like l WHERE l.review_id = r.id) AS likeCount,
                   0 AS liked
            FROM tb_review r
            INNER JOIN tb_user u ON u.id = r.user_id
            WHERE r.user_id = #{userId}
            ORDER BY r.create_time DESC
            """)
    IPage<ReviewVO> selectMyReviewPage(Page<ReviewVO> page, @Param("userId") Long userId);

    /**
     * 管理员审核列表分页（可按状态过滤；0待审核 1已通过 2已驳回）
     */
    @Select("""
            <script>
            SELECT r.id, r.shop_id AS shopId, r.user_id AS userId,
                   u.nickname, u.avatar,
                   r.title, r.content, r.images, r.score, r.status,
                   r.audit_remark AS auditRemark,
                   r.create_time AS createTime,
                   (SELECT COUNT(*) FROM tb_review_like l WHERE l.review_id = r.id) AS likeCount,
                   0 AS liked
            FROM tb_review r
            INNER JOIN tb_user u ON u.id = r.user_id
            <if test="status != null">
                WHERE r.status = #{status}
            </if>
            ORDER BY r.create_time DESC
            </script>
            """)
    IPage<ReviewVO> selectAllReviewPage(Page<ReviewVO> page, @Param("status") Integer status);
}
