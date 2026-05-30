package com.blog.personalblogbackend.idempotency;

import com.blog.personalblogbackend.config.interaction.InteractionProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.function.Supplier;

@Service
public class WriteIdempotencyService {
    private static final String LOCK_PREFIX = "idempotency:lock:";
    private static final String RESULT_PREFIX = "idempotency:result:";

    private final StringRedisTemplate redis;
    private final InteractionProperties properties;
    private final ObjectMapper objectMapper;

    public WriteIdempotencyService(StringRedisTemplate redis,
                                   InteractionProperties properties,
                                   ObjectMapper objectMapper) {
        this.redis = redis;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public static String resolveKey(String headerKey, String fallbackKey) {
        if (StringUtils.hasText(headerKey)) {
            return headerKey.trim();
        }
        return fallbackKey;
    }

    public <T> T execute(String scope, String key, Class<T> type, Supplier<T> action) {
        if (!StringUtils.hasText(key)) {
            return action.get();
        }
        String lockKey = LOCK_PREFIX + scope + ":" + key;
        String resultKey = RESULT_PREFIX + scope + ":" + key;
        Duration ttl = Duration.ofSeconds(properties.getIdempotencyTtlSeconds());

        String cached = redis.opsForValue().get(resultKey);
        if (StringUtils.hasText(cached)) {
            return read(cached, type);
        }

        Boolean claimed = redis.opsForValue().setIfAbsent(lockKey, "1", ttl);
        if (Boolean.FALSE.equals(claimed)) {
            for (int i = 0; i < 20; i++) {
                cached = redis.opsForValue().get(resultKey);
                if (StringUtils.hasText(cached)) {
                    return read(cached, type);
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            cached = redis.opsForValue().get(resultKey);
            if (StringUtils.hasText(cached)) {
                return read(cached, type);
            }
            return action.get();
        }

        try {
            T result = action.get();
            try {
                redis.opsForValue().set(resultKey, objectMapper.writeValueAsString(result), ttl);
            } catch (Exception ignored) {
            }
            return result;
        } finally {
            redis.delete(lockKey);
        }
    }

    public void storeResult(String scope, String key, Object result) {
        if (!StringUtils.hasText(key) || result == null) {
            return;
        }
        try {
            redis.opsForValue().set(RESULT_PREFIX + scope + ":" + key,
                    objectMapper.writeValueAsString(result),
                    Duration.ofSeconds(properties.getIdempotencyTtlSeconds()));
        } catch (Exception ignored) {
        }
    }

    public String readRawResult(String scope, String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        return redis.opsForValue().get(RESULT_PREFIX + scope + ":" + key);
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            return null;
        }
    }

    public <T> T readResult(String scope, String key, TypeReference<T> type) {
        String raw = readRawResult(scope, key);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, type);
        } catch (Exception e) {
            return null;
        }
    }
}
