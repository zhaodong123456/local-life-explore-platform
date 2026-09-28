package com.explore.platform.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置属性
 *
 * @author explore-platform
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * 签名密钥（HS256 至少 32 字节）
     */
    private String secret;

    /**
     * Token 有效期（毫秒）
     */
    private Long expiration;
}
