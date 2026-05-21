package com.trustplatform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.List;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;

    @Bean
    @Primary
    public CacheManager cacheManager() {
        if (redisConnectionFactory != null) {
            try {
                // Test Redis connection
                redisConnectionFactory.getConnection().close();
                log.info("Redis connection successful. Configuring RedisCacheManager.");
                
                RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(60))
                        .disableCachingNullValues();

                return RedisCacheManager.builder(redisConnectionFactory)
                        .cacheDefaults(config)
                        .build();
            } catch (Exception e) {
                log.warn("Redis is configured but connection failed. Falling back to ConcurrentMapCacheManager. Error: {}", e.getMessage());
            }
        } else {
            log.info("Redis is inactive. Fallback in-memory ConcurrentMapCacheManager is active.");
        }

        ConcurrentMapCacheManager mapCacheManager = new ConcurrentMapCacheManager();
        mapCacheManager.setCacheNames(List.of("cms", "events", "milestones", "stories"));
        return mapCacheManager;
    }
}
