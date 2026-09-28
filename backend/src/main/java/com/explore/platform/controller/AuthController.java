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

    private final AuthenticationManager authenticationManager;

    private final JwtTokenProvider jwtTokenProvider;

    private final UserService userService;

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
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getPhone(), request.getPassword()));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(loginUser);

        LoginResponse response = new LoginResponse(
                token,
                "Bearer",
                loginUser.getUser().getId(),
                loginUser.getUser().getNickname(),
                loginUser.getRoles());
        return Result.ok(response);
    }
}
