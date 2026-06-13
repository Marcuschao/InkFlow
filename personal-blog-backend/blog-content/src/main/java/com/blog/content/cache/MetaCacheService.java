package com.blog.content.cache;

import com.blog.content.config.cache.CacheProperties;
import com.blog.content.model.entity.Category;
import com.blog.content.model.entity.Tag;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MetaCacheService {
    private static final String TAGS_KEY = "meta:tags";
    private static final String CATEGORIES_KEY = "meta:categories";
    private static final String L1_TAGS = "meta-tags";
    private static final String L1_CATEGORIES = "meta-categories";

    private final LocalCacheManager localCacheManager;
    private final StringRedisTemplate redis;
    private final CacheProperties properties;
    private final CacheMetricsRecorder metrics;
    private final ObjectMapper objectMapper;

    public MetaCacheService(LocalCacheManager localCacheManager,
                            StringRedisTemplate redis,
                            CacheProperties properties,
                            CacheMetricsRecorder metrics,
                            ObjectMapper objectMapper) {
        this.localCacheManager = localCacheManager;
        this.redis = redis;
        this.properties = properties;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
    }

    public List<Tag> getTags() {
        return getList(L1_TAGS, TAGS_KEY, new TypeReference<>() {});
    }

    public void putTags(List<Tag> tags) {
        putList(L1_TAGS, TAGS_KEY, tags);
    }

    public List<Category> getCategories() {
        return getList(L1_CATEGORIES, CATEGORIES_KEY, new TypeReference<>() {});
    }

    public void putCategories(List<Category> categories) {
        putList(L1_CATEGORIES, CATEGORIES_KEY, categories);
    }

    public void evictTags() {
        localCacheManager.evictAll(L1_TAGS);
        redis.delete(TAGS_KEY);
    }

    public void evictCategories() {
        localCacheManager.evictAll(L1_CATEGORIES);
        redis.delete(CATEGORIES_KEY);
    }

    private <T> T getList(String l1Name, String redisKey, TypeReference<T> type) {
        T l1 = localCacheManager.get(l1Name, redisKey);
        if (l1 != null) {
            metrics.recordCaffeineHit();
            return l1;
        }
        metrics.recordCaffeineMiss();
        String raw = redis.opsForValue().get(redisKey);
        if (raw == null) {
            metrics.recordRedisMiss();
            return null;
        }
        metrics.recordRedisHit();
        try {
            T value = objectMapper.readValue(raw, type);
            localCacheManager.put(l1Name, redisKey, value);
            return value;
        } catch (Exception e) {
            redis.delete(redisKey);
            return null;
        }
    }

    private void putList(String l1Name, String redisKey, Object value) {
        localCacheManager.put(l1Name, redisKey, value);
        try {
            redis.opsForValue().set(redisKey, objectMapper.writeValueAsString(value),
                    properties.getL1WriteTtlMinutes(), TimeUnit.MINUTES);
        } catch (Exception ignored) {
        }
    }
}
