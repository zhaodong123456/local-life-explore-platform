package com.explore.platform.security;

import com.explore.platform.common.exception.BusinessException;
import com.explore.platform.common.result.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具：获取当前登录用户ID
 *
 * @author explore-platform
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户ID（JWT 过滤器中把 userId 放入 principal）
     */
    public static Long getCurrentUserId() {
        Long userId = getCurrentUserIdOrNull();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 获取当前登录用户ID，未登录返回 null。
     * 用于"公开接口但登录后展示个性化信息"的场景（如点评列表的 liked 标记）。
     */
    public static Long getCurrentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            return null;
        }
        return userId;
    }
}
