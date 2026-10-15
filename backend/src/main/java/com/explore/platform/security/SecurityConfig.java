package com.explore.platform.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security 安全配置
 * <p>
 * 无状态 JWT + RBAC：
 * - 登录、接口文档资源放行；
 * - 其余 /api/** 接口必须认证；
 * - 方法级权限使用 @PreAuthorize("hasRole('admin')") 等，由 @EnableMethodSecurity 开启。
 *
 * @author explore-platform
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)//开启方法级权限校验
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final CorsConfigurationSource corsConfigurationSource;

    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    private final RestAccessDeniedHandler accessDeniedHandler;

    /**
     * Knife4j / OpenAPI 相关放行路径
     */
    private static final String[] DOC_WHITELIST = {
            "/doc.html",
            "/doc.html/**",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/favicon.ico"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 前后端分离 + JWT，无需 CSRF
                .csrf(AbstractHttpConfigurer::disable)
                // 全局跨域（复用 CorsConfig 中声明的 Source）
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // 不创建 / 使用 HttpSession
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        // 公开商铺浏览（列表/详情/券列表/点评分页）仅限 GET；
                        // POST 发布点评等写操作仍需认证，商家管理端 /api/merchant/shops 不受影响
                        .requestMatchers(HttpMethod.GET, "/api/shops/**").permitAll()
                        .requestMatchers(DOC_WHITELIST).permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                // 未认证返回401 / 权限不足返回403（统一JSON）
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // JWT 过滤器放在用户名密码认证过滤器之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
