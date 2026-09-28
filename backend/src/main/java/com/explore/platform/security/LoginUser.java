package com.explore.platform.security;

import com.explore.platform.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security 使用的登录用户主体
 *
 * @author explore-platform
 */
@Getter
public class LoginUser implements UserDetails {

    private final User user;

    /**
     * 角色编码列表（不含 ROLE_ 前缀），如 ["user"] 或 ["user", "merchant"]
     */
    private final List<String> roles;

    public LoginUser(User user, List<String> roles) {
        this.user = user;
        this.roles = roles;
    }

    /**
     * 权限集合：统一加 ROLE_ 前缀，配合 hasRole('admin') 等表达式使用
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getPhone();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // status=1 正常，0 冻结
        return user.getStatus() != null && user.getStatus() == 1;
    }
}
