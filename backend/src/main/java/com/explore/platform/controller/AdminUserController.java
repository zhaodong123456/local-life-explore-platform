package com.explore.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.common.result.Result;
import com.explore.platform.service.UserService;
import com.explore.platform.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员 - 账号管理接口
 *
 * @author explore-platform
 */
@Tag(name = "管理员-账号管理")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class AdminUserController {

    private final UserService userService;

    @Operation(summary = "账号分页列表（keyword 按手机号/昵称模糊查询）")
    @GetMapping
    public Result<IPage<UserVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      @RequestParam(required = false) String keyword) {
        return Result.ok(userService.pageUsers(pageNum, pageSize, keyword));
    }
}
