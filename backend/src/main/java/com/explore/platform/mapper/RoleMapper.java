package com.explore.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.explore.platform.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色 Mapper
 *
 * @author explore-platform
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 查询某用户被授予的全部角色（RBAC 联表查询）
     */
    @Select("""
            SELECT r.*
            FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND r.status = 1
            """)
    List<Role> selectRolesByUserId(Long userId);

    /**
     * 根据角色编码查询角色
     */
    @Select("SELECT * FROM sys_role WHERE role_code = #{roleCode} AND status = 1")
    Role selectByCode(String roleCode);
}
