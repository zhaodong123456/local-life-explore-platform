package com.explore.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.common.result.Result;
import com.explore.platform.dto.MerchantAuditRequest;
import com.explore.platform.security.SecurityUtils;
import com.explore.platform.service.MerchantService;
import com.explore.platform.vo.MerchantVO;
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
 * 管理员 - 入驻审核接口
 *
 * @author explore-platform
 */
@Tag(name = "管理员-入驻审核")
@RestController
@RequestMapping("/api/admin/merchants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class AdminMerchantController {

    private final MerchantService merchantService;

    @Operation(summary = "入驻申请分页列表（可按状态过滤）")
    @GetMapping
    public Result<IPage<MerchantVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        return Result.ok(merchantService.pageMerchants(pageNum, pageSize, status));
    }

    @Operation(summary = "审核入驻（通过则授予商家角色）")
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id,
                              @Valid @RequestBody MerchantAuditRequest request) {
        merchantService.audit(SecurityUtils.getCurrentUserId(), id, request);
        return Result.ok();
    }
}
