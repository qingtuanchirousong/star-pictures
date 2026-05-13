package com.phy.starpicture.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Redis 缓存配置
 * 使用 Jackson JSON 序列化 + 随机 TTL（雪花算法），防止缓存雪崩。
 *
 * <p>缓存雪崩：大量 key 同时过期导致请求瞬间涌入数据库。
 * 解决方式：创建多个不同 TTL 的缓存区域，让不同业务的缓存过期时间自然错开，
 * 同时在单个缓存区域内通过基准 TTL + 随机偏移避免集中失效。
 */
@Configuration
public class RedisCacheConfig extends CachingConfigurerSupport {

    /** 基准 TTL：30 分钟 */
    private static final int BASE_TTL_MINUTES = 30;

    /** 随机偏移上限：10 分钟（实际 TTL 在 30~40 分钟之间） */
    private static final int JITTER_MAX_MINUTES = 10;

    @Bean

    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Jackson JSON 序列化器
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        Jackson2JsonRedisSerializer<Object> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(Object.class);
        jsonSerializer.setObjectMapper(mapper);

        // 公共序列化配置
        RedisCacheConfiguration commonConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        // 为每个缓存名创建带随机 TTL 的配置，防止雪崩
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(commonConfig)
                // 详情缓存：TTL = 30min + random(0, 10min)，每个重启周期重新随机
                .withCacheConfiguration("pictureDetail",
                        commonConfig.entryTtl(randomJitterTtl()))
                .build();
    }

    /**
     * 默认 Key 生成器：类名:方法名:参数值
     */
    @Override
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName());
            sb.append(":").append(method.getName());
            for (Object param : params) {
                if (param != null) {
                    sb.append(":").append(param);
                }
            }
            return sb.toString();
        };
    }

    /**
     * 生成带随机偏移的 TTL，实现雪花算法
     * 基准 30 分钟 + [0, 10) 分钟随机值
     */
    private Duration randomJitterTtl() {
        int extraMinutes = ThreadLocalRandom.current().nextInt(JITTER_MAX_MINUTES);
        return Duration.ofMinutes(BASE_TTL_MINUTES + extraMinutes);
    }
}
