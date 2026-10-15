package com.explore.platform.controller;

import com.explore.platform.common.result.Result;
import com.explore.platform.dto.LoginRequest;
import com.explore.platform.dto.LoginResponse;
import com.explore.platform.dto.RegisterRequest;
import com.explore.platform.security.JwtTokenProvider;
import com.explore.platform.security.LoginUser;
import com.explore.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 *
 * @author explore-platform
 */
@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;//认证管理器，用于认证用户登录

    private final JwtTokenProvider jwtTokenProvider;//JWT令牌提供器，用于生成和验证JWT令牌

    private final UserService userService;//用户服务，用于用户注册和登录

    /**
     * 用户注册（公开接口，默认授予 user 角色）
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(userService.register(request));
    }

    /**
     * 统一登录入口（user / merchant / admin 三种角色共用）
     */
    @Operation(summary = "登录（三种角色统一入口）")
    @PostMapping("/login")
    //每当使用authenticate方法，Spring Security 自动调用 UserDetailsService 查数据库查询用户信息
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getPhone(), request.getPassword()));
        //authentication 是 Spring Security 提供的认证对象，包含了用户认证信息
        //getPrincipal() 方法返回认证对象的主体，这里返回的是 LoginUser 对象
        //LoginUser 是自定义的用户认证对象，包含了用户信息和权限信息
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(loginUser);

        LoginResponse response = new LoginResponse(
                token,
                "Bearer",
                loginUser.getUser().getId(),
                loginUser.getUser().getNickname(),
                loginUser.getRoles());
        return Result.ok(response);
        //前端后续每次请求：在请求头带上 Authorization: Bearer xxx.token
    }
}
