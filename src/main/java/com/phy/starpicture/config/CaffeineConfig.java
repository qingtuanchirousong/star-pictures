package com.phy.starpicture.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置
 * 作为二级缓存中的 L1 层，提供 JVM 内的高性能查询，缓存 1000 条，5 分钟过期
 */
@Configuration
public class CaffeineConfig {

    /** 本地缓存最大条目数 */
    private static final int MAX_SIZE = 1000;

    /** 本地缓存过期时间（分钟） */
    private static final int EXPIRE_MINUTES = 5;

    /**
     * Caffeine 缓存实例
     * 用作 L1 本地缓存，存放查询结果对象
     */
    @Bean
    public Cache<String, Object> localCache() {
        return Caffeine.newBuilder()
                .maximumSize(MAX_SIZE)
                .expireAfterWrite(EXPIRE_MINUTES, TimeUnit.MINUTES)
                .build();
    }
}
