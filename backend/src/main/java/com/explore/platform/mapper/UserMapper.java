package com.explore.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.explore.platform.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper
 *
 * @author explore-platform
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据手机号查询用户（登录使用）
     */
    @Select("SELECT * FROM tb_user WHERE phone = #{phone}")
    User selectByPhone(String phone);
}
