package com.explore.platform.controller;

import com.explore.platform.common.result.Result;
import com.explore.platform.security.SecurityUtils;
import com.explore.platform.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商铺收藏 / 取消收藏接口（登录用户）。
 * <p>
 * 路径挂在 /api/shops/** 下，但 Security 白名单只放行该前缀的 GET，
 * 因此 PUT / DELETE 必须携带 Token，无需额外配置。
 *
 * @author explore-platform
 */
@Tag(name = "收藏-商铺收藏")
@RestController
@RequestMapping("/api/shops/{shopId}/favorite")
@RequiredArgsConstructor
public class ShopFavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "收藏商铺（防重复）")
    @PutMapping
    public Result<Void> favorite(@PathVariable Long shopId) {
        favoriteService.favorite(SecurityUtils.getCurrentUserId(), shopId);
        return Result.ok();
    }

    @Operation(summary = "取消收藏（幂等）")
    @DeleteMapping
    public Result<Void> unfavorite(@PathVariable Long shopId) {
        favoriteService.unfavorite(SecurityUtils.getCurrentUserId(), shopId);
        return Result.ok();
    }
}
