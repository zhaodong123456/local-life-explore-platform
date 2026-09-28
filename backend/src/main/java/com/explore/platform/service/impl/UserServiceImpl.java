package com.explore.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.explore.platform.common.exception.BusinessException;
import com.explore.platform.common.result.ErrorCode;
import com.explore.platform.dto.RegisterRequest;
import com.explore.platform.dto.UserUpdateRequest;
import com.explore.platform.entity.Role;
import com.explore.platform.entity.User;
import com.explore.platform.entity.UserRole;
import com.explore.platform.mapper.RoleMapper;
import com.explore.platform.mapper.UserMapper;
import com.explore.platform.mapper.UserRoleMapper;
import com.explore.platform.service.UserService;
import com.explore.platform.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现
 *
 * @author explore-platform
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    private final UserRoleMapper userRoleMapper;

    private final PasswordEncoder passwordEncoder;

    /**
     * 注册：手机号唯一校验 → 密码 BCrypt 加密 → 建账号 → 授予 user 角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest request) {
        // 1. 手机号查重
        User existUser = userMapper.selectByPhone(request.getPhone());
        if (existUser != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该手机号已注册");
        }

        // 2. 组装用户，密码必须加密存储
        User user = new User();
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // 昵称未填时用手机号兜底
        user.setNickname(request.getNickname() == null || request.getNickname().isBlank()
                ? "用户" + request.getPhone()
                : request.getNickname());
        user.setGender(2);
        user.setStatus(1);
        userMapper.insert(user);

        // 3. 授予默认角色 user（注册不自动成为商家/管理员）
        Role userRole = roleMapper.selectByCode("user");
        if (userRole == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "默认角色不存在，请联系管理员");
        }
        UserRole relation = new UserRole();
        relation.setUserId(user.getId());
        relation.setRoleId(userRole.getId());
        userRoleMapper.insert(relation);

        return user.getId();
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = getByIdOrThrow(userId);
        return toUserVO(user);
    }

    /**
     * 修改信息：只更新非空字段，手机号/密码不在此接口修改
     */
    @Override
    public void updateCurrentUser(Long userId, UserUpdateRequest request) {
        User user = getByIdOrThrow(userId);

        boolean changed = false;
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
            changed = true;
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
            changed = true;
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
            changed = true;
        }
        if (!changed) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "没有需要修改的字段");
        }
        userMapper.updateById(user);
    }

    private User getByIdOrThrow(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        // 查询角色编码列表返回给前端
        vo.setRoles(roleMapper.selectRolesByUserId(user.getId()).stream()
                .map(Role::getRoleCode)
                .toList());
        // 密码不允许出现在返回对象中（VO 本身也没有该字段，双保险）
        return vo;
    }

    @Override
    public IPage<UserVO> pageUsers(Integer pageNum, Integer pageSize, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreateTime);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getPhone, keyword)
                    .or().like(User::getNickname, keyword));
        }
        Page<User> page = userMapper.selectPage(new Page<>(pageNum, Math.min(pageSize, 100)), wrapper);
        return page.convert(this::toUserVO);
    }
}
