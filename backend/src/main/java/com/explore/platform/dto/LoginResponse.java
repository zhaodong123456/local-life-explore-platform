package com.explore.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录成功响应
 *
 * @author explore-platform
 */
@Data
@AllArgsConstructor//全参构造函数
@Schema(description = "登录成功响应")
public class LoginResponse implements Serializable {

    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "令牌类型")
    private String tokenType;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "角色编码列表 user/merchant/admin")
    private List<String> roles;
}
