package com.explore.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.common.result.Result;
import com.explore.platform.dto.ReviewAuditRequest;
import com.explore.platform.security.SecurityUtils;
import com.explore.platform.service.ReviewService;
import com.explore.platform.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员 - 点评审核接口
 *
 * @author explore-platform
 */
@Tag(name = "管理员-点评审核")
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class AdminReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "点评审核列表分页（可按状态过滤 0待审核 1已通过 2已驳回）")
    @GetMapping
    public Result<IPage<ReviewVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(required = false) Integer status) {
        return Result.ok(reviewService.pageAllReviews(pageNum, pageSize, status));
    }

    @Operation(summary = "审核点评（通过/驳回）")
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id,
                              @Valid @RequestBody ReviewAuditRequest request) {
        reviewService.audit(SecurityUtils.getCurrentUserId(), id, request);
        return Result.ok();
    }
}
