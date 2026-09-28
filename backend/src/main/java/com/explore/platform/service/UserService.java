package com.explore.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.dto.RegisterRequest;
import com.explore.platform.dto.UserUpdateRequest;
import com.explore.platform.vo.UserVO;

/**
 * 用户服务接口
 *
 * @author explore-platform
 */
public interface UserService {

    /**
     * 用户注册：创建账号并授予默认 user 角色
     *
     * @param request 注册参数
     * @return 新用户ID
     */
    Long register(RegisterRequest request);

    /**
     * 查询当前登录用户信息（含角色）
     *
     * @param userId 当前用户ID（来自 JWT）
     * @return 用户信息
     */
    UserVO getCurrentUser(Long userId);

    /**
     * 修改当前登录用户的个人信息
     *
     * @param userId  当前用户ID
     * @param request 修改参数
     */
    void updateCurrentUser(Long userId, UserUpdateRequest request);

    /**
     * 账号管理分页（管理员；keyword 按手机号/昵称模糊匹配）
     */
    IPage<UserVO> pageUsers(Integer pageNum, Integer pageSize, String keyword);
}
