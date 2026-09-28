package com.explore.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.common.result.Result;
import com.explore.platform.dto.ShopRequest;
import com.explore.platform.security.SecurityUtils;
import com.explore.platform.service.ShopService;
import com.explore.platform.vo.ShopVO;
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
 * 商家 - 商铺管理接口
 *
 * @author explore-platform
 */
@Tag(name = "商家-商铺管理")
@RestController
@RequestMapping("/api/merchant/shops")
@RequiredArgsConstructor
@PreAuthorize("hasRole('merchant')")
public class MerchantShopController {

    private final ShopService shopService;

    @Operation(summary = "名下商铺分页列表")
    @GetMapping
    public Result<IPage<ShopVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(shopService.pageMyShops(SecurityUtils.getCurrentUserId(), pageNum, pageSize));
    }

    @Operation(summary = "新建商铺（待审核）")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody ShopRequest request) {
        return Result.ok(shopService.createShop(SecurityUtils.getCurrentUserId(), request));
    }

    @Operation(summary = "编辑商铺（归属校验）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ShopRequest request) {
        shopService.updateShop(SecurityUtils.getCurrentUserId(), id, request);
        return Result.ok();
    }

    @Operation(summary = "上架/下架商铺")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        shopService.updateShopStatus(SecurityUtils.getCurrentUserId(), id, status);
        return Result.ok();
    }
}
