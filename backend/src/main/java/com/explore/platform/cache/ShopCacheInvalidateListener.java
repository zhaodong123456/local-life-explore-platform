package com.explore.platform.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

/**
 * 商铺缓存失效广播监听器。
 * <p>
 * 任意实例执行 evict 时向频道发布 shopId，所有实例（含发布者自身，重复删除无副作用）
 * 收到后删除自己的 Caffeine 本地缓存，保证多实例下本地缓存最终一致。
 *
 * @author explore-platform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShopCacheInvalidateListener implements MessageListener {

    private final ShopCacheService shopCacheService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 发布时用 RedisTemplate 转换，value 走 JSON 序列化；这里用 String 直接反序列化 body
        String body = RedisSerializer.string().deserialize(message.getBody());
        if (body == null || body.isBlank()) {
            return;
        }
        try {
            shopCacheService.invalidateLocal(Long.parseLong(body.trim()));
        } catch (NumberFormatException e) {
            log.warn("收到非法的缓存失效消息：{}", body);
        }
    }
}
