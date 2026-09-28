package com.explore.platform.config;

import org.springframework.context.annotation.Bean;//定义一个 Bean，用于在 Spring 容器中管理
import org.springframework.context.annotation.Configuration;//配置类，用于定义 Bean 定义
import org.springframework.web.cors.CorsConfiguration;//跨域配置类，用于定义跨域请求的规则
import org.springframework.web.cors.CorsConfigurationSource;//跨域配置源，用于提供跨域配置
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;//基于 URL 的跨域配置源，用于根据不同的 URL 配置不同的跨域规则

import java.util.List;

/**
 * 全局跨域配置
 * <p>
 * 该 CorsConfigurationSource 会被 Spring Security 的 http.cors() 自动装配，
 * 因此在无状态 JWT 架构下可以同时覆盖所有受 Security 管理的接口。
 *
 * @author explore-platform
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许携带凭证（Cookie/Authorization），此时不能用 "*"，需用 OriginPattern
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
