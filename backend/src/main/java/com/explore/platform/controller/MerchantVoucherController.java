package com.explore.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.common.result.Result;
import com.explore.platform.dto.VoucherRequest;
import com.explore.platform.security.SecurityUtils;
import com.explore.platform.service.VoucherService;
import com.explore.platform.vo.VoucherVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家 - 优惠券管理接口
 *
 * @author explore-platform
 */
@Tag(name = "商家-优惠券管理")
@RestController
@RequestMapping("/api/merchant/vouchers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('merchant')")
public class MerchantVoucherController {

    private final VoucherService voucherService;

    @Operation(summary = "名下优惠券分页（跨商铺，含下架；可按商铺过滤）")
    @GetMapping
    public Result<IPage<VoucherVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                         @RequestParam(required = false) Long shopId) {
        return Result.ok(voucherService.pageMerchantVouchers(
                SecurityUtils.getCurrentUserId(), pageNum, pageSize, shopId));
    }

    @Operation(summary = "发布优惠券（普通券/秒杀券）")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody VoucherRequest request) {
        return Result.ok(voucherService.createVoucher(SecurityUtils.getCurrentUserId(), request));
    }

    @Operation(summary = "编辑优惠券（归属校验）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody VoucherRequest request) {
        voucherService.updateVoucher(SecurityUtils.getCurrentUserId(), id, request);
        return Result.ok();
    }

    @Operation(summary = "上架/下架优惠券")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        voucherService.updateVoucherStatus(SecurityUtils.getCurrentUserId(), id, status);
        return Result.ok();
    }
}
