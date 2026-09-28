package com.explore.platform.controller;

import com.explore.platform.common.result.Result;
import com.explore.platform.security.SecurityUtils;
import com.explore.platform.service.RecommendService;
import com.explore.platform.vo.ShopVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页个性化推荐接口
 * <p>
 * 登录用户读取 Redis 中的个性化推荐列表；
 * 无行为数据/新用户走全站热门榜单兜底（冷启动策略）。
 *
 * @author explore-platform
 */
@Tag(name = "推荐-首页推荐")
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @Operation(summary = "个性化推荐商铺列表（登录；冷启动兜底热门）")
    @GetMapping
    public Result<List<ShopVO>> recommend(@RequestParam(defaultValue = "10") Integer count) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.ok(recommendService.recommendForUser(userId, count));
    }

    @Operation(summary = "手动触发推荐计算（测试/运维用）")
    @PostMapping("/refresh")
    public Result<Void> refresh() {
        recommendService.refreshRecommendations();
        return Result.ok();
    }
}
