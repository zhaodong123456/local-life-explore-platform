package com.explore.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.explore.platform.entity.Favorite;
import com.explore.platform.vo.FavoriteVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 商铺收藏 Mapper
 *
 * @author explore-platform
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 是否已收藏（应用层友好提示；最终防重由复合主键保证）
     */
    @Select("SELECT COUNT(*) > 0 FROM tb_favorite WHERE user_id = #{userId} AND shop_id = #{shopId}")
    boolean exists(@Param("userId") Long userId, @Param("shopId") Long shopId);

    /**
     * 取消收藏（不存在影响行数为 0，接口仍按成功返回，保证取消幂等）
     */
    @Delete("DELETE FROM tb_favorite WHERE user_id = #{userId} AND shop_id = #{shopId}")
    int deleteFavorite(@Param("userId") Long userId, @Param("shopId") Long shopId);

    /**
     * 我的收藏分页：关联 tb_shop 带出商铺展示信息。
     * <p>
     * 使用 INNER JOIN：商铺若被物理删除则收藏自然消失（本项目实际只做状态下架，不物理删）。
     * 按收藏时间（f.create_time）倒序。
     *
     * @param page   MP 分页参数
     * @param userId 当前用户ID
     */
    @Select("""
            SELECT f.shop_id AS shopId,
                   f.create_time AS favoriteTime,
                   s.name, s.type_id AS typeId, s.type_name AS typeName,
                   s.city, s.address, s.cover_img AS coverImg,
                   s.avg_price AS avgPrice, s.status
            FROM tb_favorite f
            INNER JOIN tb_shop s ON s.id = f.shop_id
            WHERE f.user_id = #{userId}
            ORDER BY f.create_time DESC
            """)
    IPage<FavoriteVO> selectMyFavoritePage(Page<FavoriteVO> page, @Param("userId") Long userId);
}
