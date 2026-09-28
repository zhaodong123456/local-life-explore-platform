package com.explore.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.dto.VoucherRequest;
import com.explore.platform.vo.VoucherVO;

import java.util.List;

/**
 * 优惠券服务
 *
 * @author explore-platform
 */
public interface VoucherService {

    /**
     * 商家发布优惠券（普通券 / 秒杀券）
     */
    Long createVoucher(Long userId, VoucherRequest request);

    /**
     * 商家编辑优惠券（归属校验）
     */
    void updateVoucher(Long userId, Long voucherId, VoucherRequest request);

    /**
     * 商家上架 / 下架优惠券（归属校验）
     *
     * @param status 1上架 0下架
     */
    void updateVoucherStatus(Long userId, Long voucherId, Integer status);

    /**
     * 查询某商铺的优惠券列表（公开）
     */
    List<VoucherVO> listByShop(Long shopId);

    /**
     * 商家自己的优惠券分页（跨名下所有商铺，含下架；供商家后台管理页使用）
     *
     * @param shopId 可选，按名下某商铺过滤
     */
    IPage<VoucherVO> pageMerchantVouchers(Long userId, Integer pageNum, Integer pageSize, Long shopId);
}
