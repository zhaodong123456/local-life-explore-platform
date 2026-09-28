package com.explore.platform.service;

import com.explore.platform.vo.DashboardVO;

/**
 * 商家数据看板服务
 *
 * @author explore-platform
 */
public interface MerchantDashboardService {

    /**
     * 查询指定商铺最近 N 天的看板数据
     * <p>
     * 数据归属校验：商铺必须属于当前登录商家，否则拒绝（防越权）。
     *
     * @param userId 当前登录商家用户ID
     * @param shopId 商铺ID
     * @param days   天数（1~90）
     * @return 折线图数据
     */
    DashboardVO dashboard(Long userId, Long shopId, Integer days);
}
