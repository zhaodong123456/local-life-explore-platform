package com.explore.platform.service.impl;

import com.explore.platform.common.exception.BusinessException;
import com.explore.platform.entity.Shop;
import com.explore.platform.entity.ShopDailyStat;
import com.explore.platform.mapper.ShopDailyStatMapper;
import com.explore.platform.mapper.ShopMapper;
import com.explore.platform.service.MerchantDashboardService;
import com.explore.platform.service.MerchantService;
import com.explore.platform.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家数据看板服务实现
 * <p>
 * 只查预聚合统计表 tb_shop_daily_stat，不做大表实时聚合；
 * 数据归属在 Service 层校验：商铺不存在或非本人名下一律按无权处理。
 *
 * @author explore-platform
 */
@Service
@RequiredArgsConstructor
public class MerchantDashboardServiceImpl implements MerchantDashboardService {

    private final ShopDailyStatMapper shopDailyStatMapper;

    private final ShopMapper shopMapper;

    private final MerchantService merchantService;

    @Override
    public DashboardVO dashboard(Long userId, Long shopId, Integer days) {
        // 参数约束：最多看 90 天，防止一次拉全表
        int range = (days == null || days <= 0) ? 14 : Math.min(days, 90);

        // ===== 数据归属校验（防越权核心）=====
        Long merchantId = merchantService.requireMerchantId(userId);
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException("商铺不存在");
        }
        if (!shop.getMerchantId().equals(merchantId)) {
            // 不暴露他人商铺是否存在，统一按无权限处理
            throw new BusinessException("无权查看该商铺数据");
        }

        // ===== 查统计表 =====
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(range - 1L);
        List<ShopDailyStat> stats = shopDailyStatMapper.selectRange(shopId, startDate, endDate);
        Map<LocalDate, ShopDailyStat> byDate = new HashMap<>();
        for (ShopDailyStat s : stats) {
            byDate.put(s.getStatDate(), s);
        }

        // ===== 补齐缺失日期（折线图 x 轴连续）=====
        DashboardVO vo = new DashboardVO();
        vo.setShopId(shopId);
        vo.setShopName(shop.getName());
        List<String> dates = new ArrayList<>(range);
        List<Integer> visits = new ArrayList<>(range);
        List<Integer> writeoffs = new ArrayList<>(range);
        List<Integer> reviews = new ArrayList<>(range);
        List<BigDecimal> avgScores = new ArrayList<>(range);
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            dates.add(d.toString());
            ShopDailyStat s = byDate.get(d);
            visits.add(s == null || s.getVisitCount() == null ? 0 : s.getVisitCount());
            writeoffs.add(s == null || s.getWriteoffCount() == null ? 0 : s.getWriteoffCount());
            reviews.add(s == null || s.getReviewCount() == null ? 0 : s.getReviewCount());
            // 平均分缺失补 null：前端折线断点比错误地补 0 分更符合语义
            avgScores.add(s == null ? null : s.getAvgScore());
        }
        vo.setDates(dates);
        vo.setVisitCounts(visits);
        vo.setWriteoffCounts(writeoffs);
        vo.setReviewCounts(reviews);
        vo.setAvgScores(avgScores);
        return vo;
    }
}
