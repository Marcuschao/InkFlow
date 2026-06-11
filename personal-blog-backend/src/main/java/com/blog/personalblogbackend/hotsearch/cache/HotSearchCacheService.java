package com.blog.personalblogbackend.hotsearch.cache;

import com.blog.personalblogbackend.config.hotsearch.HotSearchProperties;
import com.blog.personalblogbackend.hotsearch.model.HotSearchListVo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class HotSearchCacheService {
    private static final String PREFIX = "hot-search:";

    private final StringRedisTemplate redis;
    private final HotSearchProperties properties;
    private final ObjectMapper objectMapper;

    public HotSearchCacheService(StringRedisTemplate redis,
                                 HotSearchProperties properties,
                                 ObjectMapper objectMapper) {
        this.redis = redis;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public HotSearchListVo get(String sourceId) {
        String raw = redis.opsForValue().get(key(sourceId));
        if (raw == null) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, HotSearchListVo.class);
        } catch (Exception e) {
            redis.delete(key(sourceId));
            return null;
        }
    }

    public void put(String sourceId, HotSearchListVo vo) {
        try {
            redis.opsForValue().set(key(sourceId), objectMapper.writeValueAsString(vo),
                    properties.getCacheTtlMinutes(), TimeUnit.MINUTES);
        } catch (Exception ignored) {
        }
    }

    private static String key(String sourceId) {
        return PREFIX + sourceId;
    }
}
