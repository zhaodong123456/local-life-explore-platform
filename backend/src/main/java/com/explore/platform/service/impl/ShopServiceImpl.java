package com.explore.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.explore.platform.cache.ShopCacheService;
import com.explore.platform.common.exception.BusinessException;
import com.explore.platform.dto.ShopRequest;
import com.explore.platform.entity.Shop;
import com.explore.platform.mapper.ShopMapper;
import com.explore.platform.mq.BehaviorMessage;
import com.explore.platform.mq.BehaviorProducer;
import com.explore.platform.security.SecurityUtils;
import com.explore.platform.service.MerchantService;
import com.explore.platform.service.ShopService;
import com.explore.platform.vo.ShopVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 商铺服务实现
 *
 * @author explore-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopMapper shopMapper;

    private final MerchantService merchantService;

    /**
     * 商铺多级缓存（阶段3引入）
     */
    private final ShopCacheService shopCacheService;

    /**
     * 用户行为采集（阶段4-2引入）
     */
    private final BehaviorProducer behaviorProducer;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 浏览行为防刷窗口：同一用户 10 分钟内重复浏览同一商铺只记录一次
     */
    private static final Duration BROWSE_DEDUP_WINDOW = Duration.ofMinutes(10);

    @Override
    public Long createShop(Long userId, ShopRequest request) {
        // 只有已通过审核的商家可以创建商铺
        Long merchantId = merchantService.requireMerchantId(userId);

        Shop shop = new Shop();
        BeanUtils.copyProperties(request, shop);
        shop.setMerchantId(merchantId);
        // 新建商铺默认待审核
        shop.setStatus(0);
        shopMapper.insert(shop);
        return shop.getId();
    }

    @Override
    public void updateShop(Long userId, Long shopId, ShopRequest request) {
        Shop shop = getOwnedShop(userId, shopId);
        BeanUtils.copyProperties(request, shop);
        // 编辑后重新进入待审核（管理员需复核），状态由审核流程决定
        shop.setStatus(0);
        shopMapper.updateById(shop);
        // Cache-Aside：写库成功后删两级缓存，下次读时回源重建
        shopCacheService.evict(shopId);
    }

    @Override
    public void updateShopStatus(Long userId, Long shopId, Integer status) {
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException("商铺状态参数非法");
        }
        Shop shop = getOwnedShop(userId, shopId);
        // 待审核商铺不允许直接上架
        if (status == 1 && shop.getStatus() == 0) {
            throw new BusinessException("商铺尚未通过审核");
        }
        shop.setStatus(status);
        shopMapper.updateById(shop);
        shopCacheService.evict(shopId);
    }

    @Override
    public IPage<ShopVO> pageMyShops(Long userId, Integer pageNum, Integer pageSize) {
        int finalPageSize = Math.min(pageSize, 100);
        Long merchantId = merchantService.requireMerchantId(userId);
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<Shop>()
                .eq(Shop::getMerchantId, merchantId)
                .orderByDesc(Shop::getCreateTime);
        return shopMapper.selectPage(new Page<>(pageNum, finalPageSize), wrapper).convert(this::toVO);
    }

    @Override
    public IPage<ShopVO> pagePublicShops(Integer pageNum, Integer pageSize, Integer typeId, String keyword) {
        int finalPageSize = Math.min(pageSize, 100);
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<Shop>()
                // 公开列表只展示营业中的商铺
                .eq(Shop::getStatus, 1)
                .eq(typeId != null, Shop::getTypeId, typeId)
                .and(keyword != null && !keyword.isBlank(),
                        w -> w.like(Shop::getName, keyword).or().like(Shop::getAddress, keyword))
                .orderByDesc(Shop::getCreateTime);
        return shopMapper.selectPage(new Page<>(pageNum, finalPageSize), wrapper).convert(this::toVO);
    }

    @Override
    public ShopVO getPublicShop(Long shopId) {
        // 阶段3：详情走 Caffeine + Redis 多级缓存
        ShopVO shop = shopCacheService.getById(shopId);
        if (shop == null) {
            throw new BusinessException("商铺不存在");
        }
        // 阶段4-2：登录用户浏览行为采集（MQ异步落库，不阻塞详情查询）
        collectBrowseBehavior(shopId);
        return shop;
    }

    /**
     * 采集浏览行为：Redis SETNX 窗口去重，防止刷新页面刷爆行为表
     */
    private void collectBrowseBehavior(Long shopId) {
        try {
            Long userId = SecurityUtils.getCurrentUserIdOrNull();
            if (userId == null) {
                return; // 未登录不采集
            }
            String dedupKey = "behavior:browse:" + userId + ":" + shopId;
            Boolean firstInWindow = stringRedisTemplate.opsForValue()
                    .setIfAbsent(dedupKey, "1", BROWSE_DEDUP_WINDOW);
            if (Boolean.TRUE.equals(firstInWindow)) {
                // 1浏览 权重1
                behaviorProducer.send(new BehaviorMessage(userId, shopId, 1, 1));
            }
        } catch (Exception e) {
            // 行为采集失败不影响商铺详情主流程
            log.warn("浏览行为采集失败：shopId={}", shopId, e);
        }
    }

    @Override
    public IPage<ShopVO> pageAllShops(Integer pageNum, Integer pageSize, Integer status) {
        int finalPageSize = Math.min(pageSize, 100);
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<Shop>()
                .eq(status != null, Shop::getStatus, status)
                .orderByDesc(Shop::getCreateTime);
        return shopMapper.selectPage(new Page<>(pageNum, finalPageSize), wrapper).convert(this::toVO);
    }

    @Override
    public void auditShop(Long shopId, Boolean approved, Boolean forceOffline, String remark) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException("商铺不存在");
        }
        // 强制下架优先级最高（管理员对违规商铺的操作）
        if (Boolean.TRUE.equals(forceOffline)) {
            shop.setStatus(2);
        } else if (Boolean.TRUE.equals(approved)) {
            shop.setStatus(1);
        } else if (Boolean.FALSE.equals(approved)) {
            shop.setStatus(2);
        } else {
            throw new BusinessException("审核参数非法");
        }
        shopMapper.updateById(shop);
        shopCacheService.evict(shopId);
        // 阶段4消息模块完成后，此处补发"商铺审核结果通知"
    }

    /**
     * 获取商铺并校验归属：当前商家只能操作自己名下的商铺（防越权核心）
     */
    private Shop getOwnedShop(Long userId, Long shopId) {
        Long merchantId = merchantService.requireMerchantId(userId);
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException("商铺不存在");
        }
        if (!shop.getMerchantId().equals(merchantId)) {
            // 不暴露他人资源是否存在，统一按无权限处理
            throw new BusinessException("无权操作该商铺");
        }
        return shop;
    }

    private ShopVO toVO(Shop shop) {
        ShopVO vo = new ShopVO();
        BeanUtils.copyProperties(shop, vo);
        return vo;
    }
}
