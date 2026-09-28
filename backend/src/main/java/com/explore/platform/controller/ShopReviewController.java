package com.explore.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.common.result.Result;
import com.explore.platform.dto.ReviewPublishRequest;
import com.explore.platform.security.SecurityUtils;
import com.explore.platform.service.ReviewService;
import com.explore.platform.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商铺点评接口：
 * GET 公开浏览（Security 白名单仅放行 GET /api/shops/**）；
 * POST 发布需登录。
 *
 * @author explore-platform
 */
@Tag(name = "点评-商铺点评")
@RestController
@RequestMapping("/api/shops/{shopId}/reviews")
@RequiredArgsConstructor
public class ShopReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "发布点评（待审核）")
    @PostMapping
    public Result<Long> publish(@PathVariable Long shopId,
                                @Valid @RequestBody ReviewPublishRequest request) {
        return Result.ok(reviewService.publish(SecurityUtils.getCurrentUserId(), shopId, request));
    }

    @Operation(summary = "商铺点评分页（公开，登录后带是否已赞标记）")
    @GetMapping
    public Result<IPage<ReviewVO>> page(@PathVariable Long shopId,
                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        // 未登录返回 null，VO 的 liked 恒为 false
        Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        return Result.ok(reviewService.pageShopReviews(shopId, pageNum, pageSize, currentUserId));
    }
}
