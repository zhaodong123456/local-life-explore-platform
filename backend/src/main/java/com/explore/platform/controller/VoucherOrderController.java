package com.explore.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.common.result.Result;
import com.explore.platform.security.SecurityUtils;
import com.explore.platform.service.VoucherOrderService;
import com.explore.platform.vo.VoucherOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户侧：秒杀下单 + 我的订单接口
 *
 * @author explore-platform
 */
@Tag(name = "用户-秒杀与订单")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VoucherOrderController {

    private final VoucherOrderService voucherOrderService;

    @Operation(summary = "秒杀下单（限流 + Lua扣库存 + MQ异步创建订单，返回订单ID）")
    @PostMapping("/vouchers/{id}/seckill")
    public Result<String> seckill(@PathVariable("id") Long voucherId) {
        // 雪花ID超出JS安全整数范围(2^53)，必须以字符串返回，避免前端精度丢失
        return Result.ok(String.valueOf(
                voucherOrderService.seckillVoucher(SecurityUtils.getCurrentUserId(), voucherId)));
    }

    @Operation(summary = "我的订单分页（下单时间倒序）")
    @GetMapping("/voucher-orders/me")
    public Result<IPage<VoucherOrderVO>> myOrders(@RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(voucherOrderService.pageMyOrders(SecurityUtils.getCurrentUserId(), pageNum, pageSize));
    }

    @Operation(summary = "模拟支付（仅本人未支付订单）")
    @PutMapping("/voucher-orders/{id}/pay")
    public Result<Void> pay(@PathVariable("id") Long orderId) {
        voucherOrderService.pay(SecurityUtils.getCurrentUserId(), orderId);
        return Result.ok();
    }
}
