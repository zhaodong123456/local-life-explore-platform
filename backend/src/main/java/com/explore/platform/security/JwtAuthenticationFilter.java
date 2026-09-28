package com.explore.platform.security;// 安全模块

import io.jsonwebtoken.Claims;// JWT 有效载荷（包含用户信息）
import jakarta.servlet.FilterChain;// 过滤器链
import jakarta.servlet.ServletException;// 服务器异常   
import jakarta.servlet.http.HttpServletRequest;// HTTP 请求对象
import jakarta.servlet.http.HttpServletResponse;// HTTP 响应对象
import lombok.RequiredArgsConstructor;// 构造函数注入 final 字段
import lombok.extern.slf4j.Slf4j;// 日志记录器
import org.springframework.lang.NonNull;// 非空注解
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;// 密码认证令牌
import org.springframework.security.core.GrantedAuthority;// 授权接口，定义用户角色
import org.springframework.security.core.authority.SimpleGrantedAuthority;// 简单授权实现，用于存储用户角色
import org.springframework.security.core.context.SecurityContextHolder;// 安全上下文，存储认证信息
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;// Web 认证详情源，用于获取用户认证详情
import org.springframework.stereotype.Component;// 组件注解，用于将类标记为 Spring 组件
import org.springframework.util.StringUtils;// 字符串工具类，用于处理字符串操作
import org.springframework.web.filter.OncePerRequestFilter;// 一次每个请求过滤器，确保每个请求只被处理一次

import java.io.IOException;// I/O 操作异常
import java.util.List;// 列表接口，用于存储多个元素 

import javax.swing.Spring;

/**
 * JWT 认证过滤器：每个请求解析一次 Token，构建 SecurityContext 认证信息
 *
 * @author explore-platform
 */
@Slf4j// 日志记录器
@Component// 组件注解，用于将类标记为 Spring 组件
@RequiredArgsConstructor// 构造函数注入 final 字段
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";// 认证头名称

    private static final String BEARER_PREFIX = "Bearer ";// 认证头前缀

    private final JwtTokenProvider jwtTokenProvider;
    // 每个请求解析一次 Token，构建 SecurityContext 认证信息
    // 未认证请求（如登录接口）直接放行，后续业务接口根据 SecurityContextHolder 认证信息判断是否需要认证
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
                                        //FilterChain：过滤器链，用于调用后续过滤器处理请求
                                        //ServletException：过滤器链中某个过滤器抛出异常，导致请求处理中断
                                        //IOException：I/O 操作异常，如读取请求体失败、写入响应体失败等
        String token = resolveToken(request);
        //StringUtils：字符串工具类，用于处理字符串操作
        //hasText()：判断字符串是否非空且非空白字符
        //SecurityContextHolder.getContext().getAuthentication() == null：判断当前认证信息是否为 null，即未认证
        //若未认证则进行认证，否则直接放行
        if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            //SecurityContextHolder：安全上下文，存储认证信息
            //getContext()：获取当前线程的安全上下文
            //getAuthentication()：获取当前认证信息，若未认证则返回 null
            try {
                //Claims：JWT 有效载荷，包含用户信息
                //parseToken()：解析 JWT Token，返回有效载荷
                Claims claims = jwtTokenProvider.parseToken(token);
                //getSubject()：获取有效载荷中的主题，通常为用户 ID
                Long userId = Long.valueOf(claims.getSubject());
                //extractRoles()：从有效载荷中提取用户角色
                //roles：用户角色列表，用于授权用户访问受保护的资源
                //一个用户同时是管理员 + 普通用户：["admin", "user"]，是一个列表，所以字段叫 roles
                List<String> roles = jwtTokenProvider.extractRoles(claims);
                //GrantedAuthority：授权接口，定义用户角色
                //SimpleGrantedAuthority：简单授权实现，用于存储用户角色
                //map()：将角色列表映射为 SimpleGrantedAuthority 列表
                //toList()：将流转换为列表
                //ROLE_ 前缀：角色名称前缀，用于标识角色类型
                //SimpleGrantedAuthority("ROLE_" + role)：创建 SimpleGrantedAuthority 实例，存储角色名称
                List<GrantedAuthority> authorities = roles.stream()
                        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();

                // principal 直接放用户ID，后续业务接口可通过 @AuthenticationPrincipal / 工具类获取
                // authorities：用户角色列表，用于授权用户访问受保护的资源
                //UsernamePasswordAuthenticationToken：用户名密码认证令牌，包含用户 ID、角色列表
                //null：密码为空，因为 JWT 认证不使用密码
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                        //setDetails()：设置认证详情，用于存储用户认证信息
                        //WebAuthenticationDetailsSource：Web 认证详情源，用于获取用户认证详情
                        //buildDetails()：根据请求构建认证详情，包含用户 IP、请求头等信息
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                //setAuthentication()：设置当前认证信息，用于后续权限校验
                //SecurityContextHolder：安全上下文，存储认证信息
                //getContext()：获取当前线程的安全上下文
                //setAuthentication()：设置当前认证信息
                //放入安全上下文，后续接口可以直接拿到登录用户
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // Token 非法或过期：不设置认证信息，后续由 Security 按未认证处理（401）
                log.warn("JWT 解析失败：{}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        // 放行请求，继续处理后续过滤器
        // 未认证请求（如登录接口）直接放行，后续业务接口根据 SecurityContextHolder 认证信息判断是否需要认证
        filterChain.doFilter(request, response);
    }
    // 从请求头中提取 Token，移除前缀 "Bearer "
    // 未认证请求（如登录接口）直接放行，后续业务接口根据 SecurityContextHolder 认证信息判断是否需要认证
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
    //问题
    //1. 为什么需要从有效载荷中提取用户角色？
//用户登录成功，后端生成 JWT，把 userId、roles 放进 payload（Claims），发给前端；
// 前端后续请求接口，在请求头带上这个 Token；
// 过滤器拦截请求，解析 Token 拿到 claims，取出 userId 和 roles；
// 把 roles 转成 GrantedAuthority，组装成 Authentication 放进 Security 上下文；
// Spring Security 的注解 @PreAuthorize("hasAuthority('admin')") 或者拦截规则，去上下文读取角色，判断是否放行接口。
    //2. 为什么需要将角色列表映射为 SimpleGrantedAuthority 列表？
// Spring Security 的鉴权体系，只认识 GrantedAuthority 接口类型，不认识原始字符串角色。
// SimpleGrantedAuthority 是它最常用的实现类，作用就是把角色字符串包装成框架能识别的权限对象。
    //3. 为什么需要设置认证详情？
}
