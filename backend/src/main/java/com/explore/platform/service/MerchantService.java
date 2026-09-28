package com.explore.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.dto.MerchantApplyRequest;
import com.explore.platform.dto.MerchantAuditRequest;
import com.explore.platform.vo.MerchantVO;

/**
 * 商家入驻服务
 *
 * @author explore-platform
 */
public interface MerchantService {

    /**
     * 提交入驻申请
     *
     * @param userId  当前用户ID
     * @param request 申请信息
     * @return 商家记录ID
     */
    Long apply(Long userId, MerchantApplyRequest request);

    /**
     * 查询当前用户的入驻资料 / 审核状态
     */
    MerchantVO getMyMerchant(Long userId);

    /**
     * 管理员分页查询入驻申请（可按状态过滤）
     */
    IPage<MerchantVO> pageMerchants(Integer pageNum, Integer pageSize, Integer status);

    /**
     * 管理员审核入驻：通过则授予 merchant 角色
     */
    void audit(Long adminUserId, Long merchantId, MerchantAuditRequest request);

    /**
     * 获取当前用户已审核通过的商家ID（供商铺/优惠券模块做归属校验）
     *
     * @throws com.explore.platform.common.exception.BusinessException 非商家时抛出
     */
    Long requireMerchantId(Long userId);
}
