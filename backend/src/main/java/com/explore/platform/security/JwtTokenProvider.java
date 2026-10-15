package com.explore.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 令牌工具：签发与解析
 *
 * @author explore-platform
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;//JWT属性，包含签名密钥和有效期

    private SecretKey secretKey;//签名密钥，用于生成和验证JWT令牌

    @PostConstruct//PostConstruct注解，用于在 bean 初始化完成后执行初始化操作
    public void init() {//初始化签名密钥
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发 JWT
     *
     * @param loginUser 认证通过的登录用户
     * @return token 字符串
     */
    public String generateToken(LoginUser loginUser) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + jwtProperties.getExpiration());
        return Jwts.builder()//builder 方法用于创建 JWT 构建器
                // subject 存放用户ID
                .subject(String.valueOf(loginUser.getUser().getId()))
                //claim 存放用户手机号
                .claim("phone", loginUser.getUser().getPhone())
                // 角色编码列表：user / merchant / admin
                .claim("roles", loginUser.getRoles())
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析并校验 JWT
     *
     * @param token token 字符串
     * @return Claims
     * @throws JwtException token 非法或已过期时抛出
     */
    public Claims parseToken(String token) {//解析并校验 JWT
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Claims 中取角色编码列表
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {//从 Claims 中取角色编码列表
        Object roles = claims.get("roles");
        return roles == null ? List.of() : (List<String>) roles;
    }
}
