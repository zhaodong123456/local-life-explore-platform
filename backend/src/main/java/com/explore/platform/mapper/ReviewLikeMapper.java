package com.explore.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.explore.platform.entity.ReviewLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 点评点赞 Mapper
 *
 * @author explore-platform
 */
@Mapper
public interface ReviewLikeMapper extends BaseMapper<ReviewLike> {

    /**
     * 是否已存在点赞记录（应用层友好提示用；最终防重由复合主键保证）
     */
    @Select("SELECT COUNT(*) > 0 FROM tb_review_like WHERE user_id = #{userId} AND review_id = #{reviewId}")
    boolean exists(@Param("userId") Long userId, @Param("reviewId") Long reviewId);

    /**
     * 删除点赞记录（不存在时影响行数为 0，接口仍按成功返回，保证取消操作幂等）
     */
    @Delete("DELETE FROM tb_review_like WHERE user_id = #{userId} AND review_id = #{reviewId}")
    int deleteLike(@Param("userId") Long userId, @Param("reviewId") Long reviewId);
}
