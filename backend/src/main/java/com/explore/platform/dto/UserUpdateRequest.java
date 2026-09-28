package com.explore.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 修改个人信息请求参数
 *
 * @author explore-platform
 */
@Data
@Schema(description = "修改个人信息请求参数")
public class UserUpdateRequest implements Serializable {

    @Schema(description = "昵称")
    @Size(max = 64, message = "昵称最长 64 个字符")
    private String nickname;

    @Schema(description = "头像URL")
    @Size(max = 255, message = "头像地址过长")
    private String avatar;

    @Schema(description = "性别 0男 1女 2未知")
    @Max(value = 2, message = "性别参数非法")
    private Integer gender;
}
