package com.arth.bot.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Slf4j
@Configuration
@EnableCaching
public class RedisCacheConfig {

    private final RedisConnectionFactory redisConnectionFactory;

    public RedisCacheConfig(RedisConnectionFactory redisConnectionFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
    }

    /**
     * 检查 Redis 连接
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkRedisConnection() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.ping();
            log.info("[redis] redis connection successful");
        } catch (Exception e) {
            log.error("[redis] redis connection failed!", e);
        }
    }
}