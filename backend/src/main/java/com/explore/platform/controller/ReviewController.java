package com.explore.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.common.result.Result;
import com.explore.platform.security.SecurityUtils;
import com.explore.platform.service.ReviewService;
import com.explore.platform.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 点评接口：我的点评 + 点赞 / 取消点赞（均需登录）
 *
 * @author explore-platform
 */
@Tag(name = "点评-我的点评与点赞")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "我的点评分页（含待审核/驳回状态）")
    @GetMapping("/me")
    public Result<IPage<ReviewVO>> myReviews(@RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(reviewService.pageMyReviews(SecurityUtils.getCurrentUserId(), pageNum, pageSize));
    }

    @Operation(summary = "点赞（防重复）")
    @PutMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id) {
        reviewService.like(SecurityUtils.getCurrentUserId(), id);
        return Result.ok();
    }

    @Operation(summary = "取消点赞（幂等）")
    @DeleteMapping("/{id}/like")
    public Result<Void> unlike(@PathVariable Long id) {
        reviewService.unlike(SecurityUtils.getCurrentUserId(), id);
        return Result.ok();
    }
}
