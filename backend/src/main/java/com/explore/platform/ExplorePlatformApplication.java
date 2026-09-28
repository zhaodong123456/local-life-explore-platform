package com.explore.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 本地生活探店平台启动类
 *
 * @author explore-platform
 */
@SpringBootApplication
@MapperScan("com.explore.platform.mapper")
// 扫描根包下所有 @ConfigurationProperties（security.JwtProperties、config.ShopCacheProperties）
@ConfigurationPropertiesScan
// 开启定时任务（阶段4-2：推荐列表定时计算）
@EnableScheduling
public class ExplorePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExplorePlatformApplication.class, args);
    }
}
