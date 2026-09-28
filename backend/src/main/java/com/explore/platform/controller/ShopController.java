package com.explore.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.common.result.Result;
import com.explore.platform.service.ShopService;
import com.explore.platform.service.VoucherService;
import com.explore.platform.vo.ShopVO;
import com.explore.platform.vo.VoucherVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开商铺接口
 * <p>
 * 当前直接查库；阶段 3 将改造详情/列表为 Caffeine + Redis 多级缓存。
 *
 * @author explore-platform
 */
@Tag(name = "公开商铺接口")
@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    private final VoucherService voucherService;

    @Operation(summary = "商铺分页列表（类型/关键字）")
    @GetMapping
    public Result<IPage<ShopVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      @RequestParam(required = false) Integer typeId,
                                      @RequestParam(required = false) String keyword) {
        return Result.ok(shopService.pagePublicShops(pageNum, pageSize, typeId, keyword));
    }

    @Operation(summary = "商铺详情")
    @GetMapping("/{id}")
    public Result<ShopVO> detail(@PathVariable Long id) {
        return Result.ok(shopService.getPublicShop(id));
    }

    @Operation(summary = "商铺的优惠券列表")
    @GetMapping("/{id}/vouchers")
    public Result<List<VoucherVO>> vouchers(@PathVariable Long id) {
        return Result.ok(voucherService.listByShop(id));
    }
}
