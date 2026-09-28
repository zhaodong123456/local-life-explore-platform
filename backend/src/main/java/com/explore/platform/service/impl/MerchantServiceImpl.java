package com.explore.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.explore.platform.common.exception.BusinessException;
import com.explore.platform.dto.MerchantApplyRequest;
import com.explore.platform.dto.MerchantAuditRequest;
import com.explore.platform.entity.Merchant;
import com.explore.platform.entity.Role;
import com.explore.platform.entity.UserRole;
import com.explore.platform.mapper.MerchantMapper;
import com.explore.platform.mapper.RoleMapper;
import com.explore.platform.mapper.UserRoleMapper;
import com.explore.platform.mq.NotifyMessage;
import com.explore.platform.mq.NotifyMessageProducer;
import com.explore.platform.service.MerchantService;
import com.explore.platform.vo.MerchantVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 商家入驻服务实现
 *
 * @author explore-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;

    private final RoleMapper roleMapper;

    private final UserRoleMapper userRoleMapper;

    private final NotifyMessageProducer notifyMessageProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long apply(Long userId, MerchantApplyRequest request) {
        Merchant exist = merchantMapper.selectByUserId(userId);

        // 已有申请记录：按状态处理
        if (exist != null) {
            if (exist.getStatus() == 0) {
                throw new BusinessException("入驻申请正在审核中，请勿重复提交");
            }
            if (exist.getStatus() == 1) {
                throw new BusinessException("您已是入驻商家，无需重复申请");
            }
            // status=2 已驳回：允许修改资料后重新提交，复用原记录
            BeanUtils.copyProperties(request, exist);
            exist.setStatus(0);
            exist.setAuditAdminId(null);
            exist.setAuditTime(null);
            exist.setAuditRemark(null);
            merchantMapper.updateById(exist);
            return exist.getId();
        }

        Merchant merchant = new Merchant();
        merchant.setUserId(userId);
        BeanUtils.copyProperties(request, merchant);
        merchant.setStatus(0);
        merchantMapper.insert(merchant);
        return merchant.getId();
    }

    @Override
    public MerchantVO getMyMerchant(Long userId) {
        Merchant merchant = merchantMapper.selectByUserId(userId);
        if (merchant == null) {
            throw new BusinessException("尚未提交入驻申请");
        }
        return toVO(merchant);
    }

    @Override
    public IPage<MerchantVO> pageMerchants(Integer pageNum, Integer pageSize, Integer status) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<Merchant>()
                .eq(status != null, Merchant::getStatus, status)
                .orderByDesc(Merchant::getCreateTime);
        IPage<Merchant> page = merchantMapper.selectPage(new Page<>(pageNum, Math.min(pageSize, 100)), wrapper);
        // 实体页转 VO 页
        return page.convert(this::toVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long adminUserId, Long merchantId, MerchantAuditRequest request) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("入驻申请不存在");
        }
        if (merchant.getStatus() != 0) {
            throw new BusinessException("该申请已审核，请勿重复操作");
        }

        boolean approved = Boolean.TRUE.equals(request.getApproved());
        merchant.setAuditAdminId(adminUserId);
        merchant.setAuditTime(LocalDateTime.now());
        merchant.setAuditRemark(request.getRemark());

        if (approved) {
            merchant.setStatus(1);
            merchantMapper.updateById(merchant);
            // 审核通过：授予 merchant 角色（幂等，已有该角色则跳过）
            grantMerchantRole(merchant.getUserId());
        } else {
            merchant.setStatus(2);
            merchantMapper.updateById(merchant);
        }
        // 阶段4-1：给申请人发"入驻审核结果"消息（type=3）
        try {
            notifyMessageProducer.send(new NotifyMessage(
                    merchant.getUserId(),
                    3,
                    approved ? "入驻审核通过" : "入驻审核未通过",
                    approved
                            ? "恭喜！您的商家入驻申请已通过审核，快去管理您的商铺吧~"
                            : "很抱歉，您的入驻申请未通过审核，原因：" + (request.getRemark() != null ? request.getRemark() : "资料不完整"),
                    "merchant_audit",
                    merchantId));
        } catch (Exception e) {
            log.warn("入驻审核结果消息投递失败：merchantId={}", merchantId, e);
        }
    }

    @Override
    public Long requireMerchantId(Long userId) {
        Merchant merchant = merchantMapper.selectByUserId(userId);
        if (merchant == null || merchant.getStatus() != 1) {
            throw new BusinessException("您还不是入驻商家");
        }
        return merchant.getId();
    }

    /**
     * 授予商家角色，通过唯一索引 + 先查保证幂等
     */
    private void grantMerchantRole(Long userId) {
        Role merchantRole = roleMapper.selectByCode("merchant");
        if (merchantRole == null) {
            throw new BusinessException("商家角色不存在，请联系管理员");
        }
        // 已拥有该角色则不重复插入
        boolean hasRole = roleMapper.selectRolesByUserId(userId).stream()
                .anyMatch(role -> "merchant".equals(role.getRoleCode()));
        if (hasRole) {
            return;
        }
        UserRole relation = new UserRole();
        relation.setUserId(userId);
        relation.setRoleId(merchantRole.getId());
        userRoleMapper.insert(relation);
    }

    private MerchantVO toVO(Merchant merchant) {
        MerchantVO vo = new MerchantVO();
        BeanUtils.copyProperties(merchant, vo);
        return vo;
    }
}
