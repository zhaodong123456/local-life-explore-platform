package com.explore.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.explore.platform.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 商家 Mapper
 *
 * @author explore-platform
 */
@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {

    /**
     * 根据用户ID查询商家资料
     */
    @Select("SELECT * FROM tb_merchant WHERE user_id = #{userId}")
    Merchant selectByUserId(Long userId);
}
