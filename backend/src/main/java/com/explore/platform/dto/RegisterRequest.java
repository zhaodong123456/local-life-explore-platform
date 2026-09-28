package com.explore.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 注册请求参数
 *
 * @author explore-platform
 */
@Data
@Schema(description = "注册请求参数")
public class RegisterRequest implements Serializable {

    @Schema(description = "手机号", example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "密码（6~32位）", example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在 6~32 位之间")
    private String password;

    @Schema(description = "昵称（不填则默认用手机号）", example = "探店达人")
    @Size(max = 64, message = "昵称最长 64 个字符")
    private String nickname;
}
