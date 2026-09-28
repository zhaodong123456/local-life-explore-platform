package com.explore.platform.security;

import com.explore.platform.entity.Role;
import com.explore.platform.entity.User;
import com.explore.platform.mapper.RoleMapper;
import com.explore.platform.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户认证服务：从数据库加载用户与角色（RBAC）
 *
 * @author explore-platform
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        User user = userMapper.selectByPhone(phone);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        // 通过 sys_user_role 关联查询角色，天然支持一人多角色
        List<Role> roleList = roleMapper.selectRolesByUserId(user.getId());
        List<String> roleCodes = roleList.stream().map(Role::getRoleCode).toList();
        return new LoginUser(user, roleCodes);
    }
}
