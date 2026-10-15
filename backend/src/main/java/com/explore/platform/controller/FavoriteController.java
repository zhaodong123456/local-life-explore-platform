package com.explore.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.common.result.Result;
import com.explore.platform.security.SecurityUtils;
import com.explore.platform.service.FavoriteService;
import com.explore.platform.vo.FavoriteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 我的收藏接口（登录用户）
 *
 * @author explore-platform
 */
@Tag(name = "收藏-我的收藏")
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor//RequiredArgsConstructor注解，用于在构造函数中注入 final 字段
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "我的收藏分页（按收藏时间倒序，带商铺摘要）")
    @GetMapping
    public Result<IPage<FavoriteVO>> myFavorites(@RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(favoriteService.pageMyFavorites(SecurityUtils.getCurrentUserId(), pageNum, pageSize));
    }
}
