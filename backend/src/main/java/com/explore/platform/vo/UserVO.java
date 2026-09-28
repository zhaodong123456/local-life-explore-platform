package com.explore.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户信息展示对象
 *
 * @author explore-platform
 */
@Data
@Schema(description = "用户信息")
public class UserVO implements Serializable {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "性别 0男 1女 2未知")
    private Integer gender;

    @Schema(description = "角色编码列表")
    private List<String> roles;
}
