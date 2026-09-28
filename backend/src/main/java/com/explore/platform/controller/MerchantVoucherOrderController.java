package com.explore.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.common.result.Result;
import com.explore.platform.security.SecurityUtils;
import com.explore.platform.service.VoucherOrderService;
import com.explore.platform.vo.VoucherOrderVO;
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
 * 商家侧：优惠券订单核销接口
 *
 * @author explore-platform
 */
@Tag(name = "商家-订单核销")
@RestController
@RequestMapping("/api/merchant/voucher-orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('merchant')")
public class MerchantVoucherOrderController {

    private final VoucherOrderService voucherOrderService;

    @Operation(summary = "名下订单分页（核销记录页数据源；可按状态过滤 1未支付 2待核销 3已核销）")
    @GetMapping
    public Result<IPage<VoucherOrderVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) Integer status) {
        return Result.ok(voucherOrderService.pageMerchantOrders(
                SecurityUtils.getCurrentUserId(), pageNum, pageSize, status));
    }

    @Operation(summary = "核销优惠券（仅本名下商铺的已支付订单）")
    @PutMapping("/{id}/writeoff")
    public Result<Void> writeOff(@PathVariable("id") Long orderId) {
        voucherOrderService.writeOff(SecurityUtils.getCurrentUserId(), orderId);
        return Result.ok();
    }
}
