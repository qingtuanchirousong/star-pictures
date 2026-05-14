package com.phy.starpicture.manager;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Redis + Caffeine 二级缓存管理器
 * L1（Caffeine）：JVM 本地缓存，5 分钟过期，极快但仅单机可见
 * L2（Redis）：分布式缓存，30 分钟过期，多实例共享
 *
 * <p>查询流程：L1 → L2 → DB，命中任一级即回填上级缓存
 * <p>清除流程：写操作时清除所有相关缓存 key，保证数据一致性
 */
@Slf4j
@Component
public class TwoLevelCacheManager {

    /** Redis key 前缀 */
    private static final String REDIS_PREFIX = "cache:";

    /** Redis 中记录所有分页缓存 key 的 Set */
    private static final String KEY_REGISTRY = "cache:picPage:keys";

    /** Redis 缓存过期时间：30 分钟 */
    private static final Duration REDIS_TTL = Duration.ofMinutes(30);

    @Resource
    private Cache<String, Object> localCache;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 从二级缓存中获取数据，未命中时调用 loader 查询并自动回填
     *
     * @param cacheKey  缓存 key（不含前缀，由本方法统一加前缀存入 Redis）
     * @param javaType  返回值类型（用于 JSON 反序列化，支持泛型如 Page&lt;PictureVO&gt;）
     * @param loader    数据库查询回调，缓存未命中时调用
     * @param <T>       返回值泛型
     * @return 缓存或数据库查询结果
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String cacheKey, JavaType javaType, Supplier<T> loader) {
        // ── L1：Caffeine 本地缓存 ──
        Object l1Value = localCache.getIfPresent(cacheKey);
        if (l1Value != null) {
            log.debug("L1 缓存命中: {}", cacheKey);
            return (T) l1Value;
        }

        // ── L2：Redis 分布式缓存 ──
        String redisKey = REDIS_PREFIX + cacheKey;
        try {
            String json = stringRedisTemplate.opsForValue().get(redisKey);
            if (json != null) {
                T value = objectMapper.readValue(json, javaType);
                localCache.put(cacheKey, value);  // 回填 L1
                log.debug("L2 缓存命中: {}", cacheKey);
                return value;
            }
        } catch (Exception e) {
            log.warn("读取 Redis 缓存异常，跳过 L2: {}", e.getMessage());
        }

        // ── DB：执行实际查询 ──
        T value = loader.get();
        if (value != null) {
            // 回填两级缓存
            localCache.put(cacheKey, value);
            try {
                String json = objectMapper.writeValueAsString(value);
                stringRedisTemplate.opsForValue().set(redisKey, json, REDIS_TTL);
                // 将 key 注册到 Set，便于后续批量清除
                stringRedisTemplate.opsForSet().add(KEY_REGISTRY, redisKey);
            } catch (Exception e) {
                log.warn("写入 Redis 缓存异常: {}", e.getMessage());
            }
            log.debug("DB 查询完成，已回填缓存: {}", cacheKey);
        }
        return value;
    }

    /**
     * 清除所有图片分页缓存
     * 图片发生增删改或审核后调用，保证下次查询拿到最新数据
     */
    public void evictPictureListCache() {
        // 清除 L1 全部分页缓存
        localCache.asMap().keySet().removeIf(key -> key.startsWith("picPage:"));

        // 清除 L2 中已注册的缓存 key
        try {
            Set<String> registeredKeys = stringRedisTemplate.opsForSet().members(KEY_REGISTRY);
            if (registeredKeys != null && !registeredKeys.isEmpty()) {
                stringRedisTemplate.delete(registeredKeys);
            }
            stringRedisTemplate.delete(KEY_REGISTRY);
            log.debug("已清除 {} 个分页缓存 key", registeredKeys != null ? registeredKeys.size() : 0);
        } catch (Exception e) {
            log.warn("清除 Redis 分页缓存异常: {}", e.getMessage());
        }
    }
}
