package com.explore.platform.controller;

import com.explore.platform.common.result.Result;
import com.explore.platform.dto.MerchantApplyRequest;
import com.explore.platform.security.SecurityUtils;
import com.explore.platform.service.MerchantService;
import com.explore.platform.vo.MerchantVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家入驻接口（登录用户均可申请）
 *
 * @author explore-platform
 */
@Tag(name = "商家入驻接口")
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @Operation(summary = "提交入驻申请")
    @PostMapping("/apply")
    public Result<Long> apply(@Valid @RequestBody MerchantApplyRequest request) {
        return Result.ok(merchantService.apply(SecurityUtils.getCurrentUserId(), request));
    }

    @Operation(summary = "查询我的入驻资料与审核状态")
    @GetMapping("/me")
    public Result<MerchantVO> me() {
        return Result.ok(merchantService.getMyMerchant(SecurityUtils.getCurrentUserId()));
    }
}
