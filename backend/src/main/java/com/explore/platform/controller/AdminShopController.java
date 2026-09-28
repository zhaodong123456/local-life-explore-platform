package com.explore.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.common.result.Result;
import com.explore.platform.service.ShopService;
import com.explore.platform.vo.ShopVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员 - 商铺审核接口
 *
 * @author explore-platform
 */
@Tag(name = "管理员-商铺审核")
@RestController
@RequestMapping("/api/admin/shops")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class AdminShopController {

    private final ShopService shopService;

    @Operation(summary = "商铺分页列表（可按状态过滤）")
    @GetMapping
    public Result<IPage<ShopVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      @RequestParam(required = false) Integer status) {
        return Result.ok(shopService.pageAllShops(pageNum, pageSize, status));
    }

    @Operation(summary = "审核通过/驳回/强制下架商铺")
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id,
                              @RequestParam(required = false) Boolean approved,
                              @RequestParam(required = false) Boolean forceOffline,
                              @RequestParam(required = false) String remark) {
        shopService.auditShop(id, approved, forceOffline, remark);
        return Result.ok();
    }
}
