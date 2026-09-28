package com.explore.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.explore.platform.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联 Mapper
 *
 * @author explore-platform
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
}
