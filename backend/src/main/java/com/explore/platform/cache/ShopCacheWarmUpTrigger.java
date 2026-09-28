package com.explore.platform.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 应用启动完成后触发商铺缓存预热。
 * <p>
 * 使用 ApplicationReadyEvent 而非 @PostConstruct：此时 Bean 全部初始化完毕、
 * 数据源与 Redis 连接均可用，预热失败也不会拖垮/阻断启动。
 *
 * @author explore-platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShopCacheWarmUpTrigger {

    private final ShopCacheService shopCacheService;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        long start = System.currentTimeMillis();
        shopCacheService.warmUp();
        log.info("缓存预热触发完成，耗时 {} ms", System.currentTimeMillis() - start);
    }
}
