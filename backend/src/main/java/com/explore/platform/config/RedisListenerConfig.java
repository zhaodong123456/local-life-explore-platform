package com.explore.platform.config;

import com.explore.platform.cache.CacheKeys;
import com.explore.platform.cache.ShopCacheInvalidateListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis 消息监听容器配置：注册商铺缓存失效广播订阅
 *
 * @author explore-platform
 */
@Configuration
public class RedisListenerConfig {

    @Bean
    RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory,
                                                                ShopCacheInvalidateListener listener) {
        //RedisConnectionFactory 连接工厂，用于创建 Redis 连接
        //ShopCacheInvalidateListener 店铺缓存失效监听器，用于处理商铺缓存失效广播
        //RedisMessageListenerContainer Redis 消息监听容器，用于监听 Redis 通道上的消息
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();//创建 Redis 消息监听容器
        container.setConnectionFactory(connectionFactory);//设置 Redis 连接工厂
        //ChannelTopic 通道主题，用于指定要监听的 Redis 通道
        //CacheKeys.SHOP_EVICT_CHANNEL 店铺缓存失效通道
        container.addMessageListener(listener, new ChannelTopic(CacheKeys.SHOP_EVICT_CHANNEL));//添加消息监听器，监听商铺缓存失效通道
        return container;
    }
}
