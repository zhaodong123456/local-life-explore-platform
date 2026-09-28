package com.explore.platform.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.explore.platform.common.exception.BusinessException;
import com.explore.platform.entity.Favorite;
import com.explore.platform.entity.Shop;
import com.explore.platform.mapper.FavoriteMapper;
import com.explore.platform.mapper.ShopMapper;
import com.explore.platform.mq.BehaviorMessage;
import com.explore.platform.mq.BehaviorProducer;
import com.explore.platform.service.FavoriteService;
import com.explore.platform.vo.FavoriteVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商铺收藏服务实现
 *
 * @author explore-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;

    private final ShopMapper shopMapper;

    private final BehaviorProducer behaviorProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favorite(Long userId, Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException("商铺不存在");
        }
        // 不强制要求商铺营业中：允许收藏后即使临时下架也保留，恢复营业后仍可查看

        // 应用层先查，给出友好提示；并发漏网时由复合主键兜底
        if (favoriteMapper.exists(userId, shopId)) {
            throw new BusinessException("您已收藏该商铺");
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setShopId(shopId);
        try {
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException e) {
            // 并发场景：复合主键 (user_id, shop_id) 拒绝重复
            throw new BusinessException("您已收藏该商铺");
        }
        // 阶段4-2：异步写用户行为（behavior_type=3 收藏，权重5）用于协同过滤推荐
        try {
            behaviorProducer.send(new BehaviorMessage(userId, shopId, 3, 5));
        } catch (Exception e) {
            // 行为采集失败不影响收藏主流程
            log.warn("收藏行为采集失败：userId={}, shopId={}", userId, shopId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavorite(Long userId, Long shopId) {
        // 删除是幂等操作，记录不存在影响行数为0
        favoriteMapper.deleteFavorite(userId, shopId);
    }

    @Override
    public IPage<FavoriteVO> pageMyFavorites(Long userId, Integer pageNum, Integer pageSize) {
        return favoriteMapper.selectMyFavoritePage(new Page<>(pageNum, Math.min(pageSize, 100)), userId);
    }
}
