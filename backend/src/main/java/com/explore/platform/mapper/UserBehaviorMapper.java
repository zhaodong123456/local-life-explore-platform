package com.explore.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.explore.platform.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户行为日志 Mapper
 *
 * @author explore-platform
 */
@Mapper
public interface UserBehaviorMapper extends BaseMapper<UserBehavior> {

    /**
     * 查询指定时间窗口内的有效行为数据（用于协同过滤计算）
     *
     * @param since 时间窗口起点（如 30 天前）
     * @return 行为列表
     */
    @Select("SELECT id, user_id, shop_id, behavior_type, weight, create_time " +
            "FROM tb_user_behavior WHERE create_time >= #{since} " +
            "ORDER BY user_id, shop_id")
    List<UserBehavior> selectRecentBehaviors(@Param("since") LocalDateTime since);

    /**
     * 查询指定用户交互过的所有商铺 ID（用于推荐过滤已交互商铺）
     *
     * @param userId 用户ID
     * @return 商铺ID列表
     */
    @Select("SELECT DISTINCT shop_id FROM tb_user_behavior WHERE user_id = #{userId}")
    List<Long> selectInteractedShopIds(@Param("userId") Long userId);
}
