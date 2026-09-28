package com.explore.platform.controller;

import com.explore.platform.common.result.Result;
import com.explore.platform.security.SecurityUtils;
import com.explore.platform.service.MerchantDashboardService;
import com.explore.platform.vo.DashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家数据看板接口（merchant 角色）
 * <p>
 * 数据来自预聚合统计表 tb_shop_daily_stat（定时任务每小时刷新昨日+今日），
 * 接口层不实时聚合大表；商铺数据归属校验在 Service 层完成。
 *
 * @author explore-platform
 */
@Tag(name = "商家-数据看板")
@RestController
@RequestMapping("/api/merchant/dashboard")
@RequiredArgsConstructor
public class MerchantDashboardController {

    private final MerchantDashboardService merchantDashboardService;

    @Operation(summary = "商铺看板数据（最近N天，折线图结构）")
    @PreAuthorize("hasRole('merchant')")
    @GetMapping("/stats")
    public Result<DashboardVO> stats(@RequestParam Long shopId,
                                     @RequestParam(defaultValue = "14") Integer days) {
        return Result.ok(merchantDashboardService.dashboard(
                SecurityUtils.getCurrentUserId(), shopId, days));
    }
}
