package com.blog.personalblogbackend.cache;

import com.blog.personalblogbackend.config.cache.CacheProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class LocalCacheManager {
    private final ConcurrentHashMap<String, Cache<String, Object>> caches = new ConcurrentHashMap<>();
    private final CacheProperties properties;

    public LocalCacheManager(CacheProperties properties) {
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String cacheName, String key) {
        Cache<String, Object> cache = caches.computeIfAbsent(cacheName, this::buildCache);
        return (T) cache.getIfPresent(key);
    }

    public void put(String cacheName, String key, Object value) {
        Cache<String, Object> cache = caches.computeIfAbsent(cacheName, this::buildCache);
        cache.put(key, value);
    }

    public void evict(String cacheName, String key) {
        Cache<String, Object> cache = caches.get(cacheName);
        if (cache != null) {
            cache.invalidate(key);
        }
    }

    public void evictAll(String cacheName) {
        Cache<String, Object> cache = caches.get(cacheName);
        if (cache != null) {
            cache.invalidateAll();
        }
    }

    private Cache<String, Object> buildCache(String name) {
        return Caffeine.newBuilder()
                .maximumSize(properties.getL1MaxSize())
                .expireAfterWrite(properties.getL1WriteTtlMinutes(), TimeUnit.MINUTES)
                .build();
    }
}
