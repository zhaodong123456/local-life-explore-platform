package com.explore.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.dto.ShopRequest;
import com.explore.platform.vo.ShopVO;

/**
 * 商铺服务
 *
 * @author explore-platform
 */
public interface ShopService {

    /**
     * 商家创建商铺（初始状态待审核）
     */
    Long createShop(Long userId, ShopRequest request);

    /**
     * 商家修改商铺（归属校验）
     */
    void updateShop(Long userId, Long shopId, ShopRequest request);

    /**
     * 商家上架 / 下架商铺（归属校验）
     *
     * @param status 1营业 2下架
     */
    void updateShopStatus(Long userId, Long shopId, Integer status);

    /**
     * 商家查询名下商铺
     */
    IPage<ShopVO> pageMyShops(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 公开商铺分页（关键字 / 类型）。阶段3在此基础上增加多级缓存
     */
    IPage<ShopVO> pagePublicShops(Integer pageNum, Integer pageSize, Integer typeId, String keyword);

    /**
     * 公开商铺详情。阶段3在此基础上增加多级缓存
     */
    ShopVO getPublicShop(Long shopId);

    /**
     * 管理员分页查询商铺（可按状态过滤）
     */
    IPage<ShopVO> pageAllShops(Integer pageNum, Integer pageSize, Integer status);

    /**
     * 管理员审核商铺 / 强制下架
     *
     * @param approved true 审核通过（营业） false 驳回；forceOffline 为 true 时强制下架
     */
    void auditShop(Long shopId, Boolean approved, Boolean forceOffline, String remark);
}
