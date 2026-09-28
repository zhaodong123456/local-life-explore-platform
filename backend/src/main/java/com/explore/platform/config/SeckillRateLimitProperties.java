package com.explore.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 秒杀本地限流配置（接口级二级限流；一级限流在网关层，见设计文档 7 架构图）
 *
 * @author explore-platform
 */
@Data
@Component
@ConfigurationProperties(prefix = "seckill.rate-limit")//配置属性前缀，用于绑定配置文件中的属性
public class SeckillRateLimitProperties {

    /**
     * 全局限流：接口整体每秒放行请求数（超出直接 429，保护下游 Redis/MQ）
     */
    private int globalQps = 500;

    /**
     * 单用户限流：每个用户在窗口内允许的请求次数（防脚本连点刷单）
     */
    private int perUserCount = 3;

    /**
     * 单用户限流窗口（秒）
     */
    private int perUserWindowSeconds = 1;
}
