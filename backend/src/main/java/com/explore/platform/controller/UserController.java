package com.explore.platform.controller;

import com.explore.platform.common.result.Result;
import com.explore.platform.dto.UserUpdateRequest;
import com.explore.platform.service.UserService;
import com.explore.platform.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户信息接口
 *
 * @author explore-platform
 */
@Tag(name = "用户信息接口")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户信息
     */
    @Operation(summary = "获取当前登录用户信息（含角色）")
    @GetMapping("/me")
    public Result<UserVO> me(@AuthenticationPrincipal Long userId) {
        return Result.ok(userService.getCurrentUser(userId));
    }

    /**
     * 修改当前登录用户信息
     */
    @Operation(summary = "修改当前登录用户信息")
    @PutMapping("/me")
    public Result<Void> updateMe(@AuthenticationPrincipal Long userId,
                                 @Valid @RequestBody UserUpdateRequest request) {
        userService.updateCurrentUser(userId, request);
        return Result.ok();
    }
}
