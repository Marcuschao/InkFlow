package com.blog.personalblogbackend.cache;

import com.blog.personalblogbackend.config.cache.CacheProperties;
import com.blog.personalblogbackend.model.entity.Article;
import com.blog.personalblogbackend.model.vo.ArticleVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ArticleCacheService {
    private static final String NULL_MARKER = "__NULL__";
    private static final String DETAIL_PREFIX = "article:detail:";
    private static final String LIST_PREFIX = "article:list:";
    private static final String L1_DETAIL = "article-detail";
    private static final String L1_LIST = "article-list";

    private final LocalCacheManager localCacheManager;
    private final StringRedisTemplate redis;
    private final CacheProperties properties;
    private final CacheMetricsRecorder metrics;
    private final ObjectMapper objectMapper;

    public ArticleCacheService(LocalCacheManager localCacheManager,
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

    public ArticleVO getDetail(Long id, String lang) {
        String key = detailKey(id, lang);
        ArticleVO l1 = localCacheManager.get(L1_DETAIL, key);
        if (l1 != null) {
            metrics.recordCaffeineHit();
            return l1;
        }
        metrics.recordCaffeineMiss();
        String raw = redis.opsForValue().get(DETAIL_PREFIX + key);
        if (raw != null) {
            metrics.recordRedisHit();
            if (NULL_MARKER.equals(raw)) {
                return null;
            }
            try {
                ArticleVO vo = objectMapper.readValue(raw, ArticleVO.class);
                localCacheManager.put(L1_DETAIL, key, vo);
                return vo;
            } catch (Exception ignored) {
                redis.delete(DETAIL_PREFIX + key);
            }
        } else {
            metrics.recordRedisMiss();
        }
        return null;
    }

    public void putDetail(Long id, String lang, ArticleVO vo) {
        String key = detailKey(id, lang);
        localCacheManager.put(L1_DETAIL, key, vo);
        try {
            String json = objectMapper.writeValueAsString(vo);
            redis.opsForValue().set(DETAIL_PREFIX + key, json,
                    properties.getL1WriteTtlMinutes(), TimeUnit.MINUTES);
        } catch (Exception ignored) {
        }
    }

    public void putDetailNull(Long id, String lang) {
        String key = detailKey(id, lang);
        localCacheManager.put(L1_DETAIL, key, null);
        redis.opsForValue().set(DETAIL_PREFIX + key, NULL_MARKER,
                properties.getNullTtlSeconds(), TimeUnit.SECONDS);
    }

    public List<Article> getList(String listKey) {
        String l1Key = LIST_PREFIX + listKey;
        List<Article> l1 = localCacheManager.get(L1_LIST, l1Key);
        if (l1 != null) {
            metrics.recordCaffeineHit();
            return l1;
        }
        metrics.recordCaffeineMiss();
        String raw = redis.opsForValue().get(l1Key);
        if (raw == null) {
            metrics.recordRedisMiss();
            return null;
        }
        metrics.recordRedisHit();
        try {
            List<Article> list = objectMapper.readValue(raw, new TypeReference<>() {});
            localCacheManager.put(L1_LIST, l1Key, list);
            return list;
        } catch (Exception e) {
            redis.delete(l1Key);
            return null;
        }
    }

    public void putList(String listKey, List<Article> articles) {
        String key = LIST_PREFIX + listKey;
        localCacheManager.put(L1_LIST, key, articles);
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(articles),
                    properties.getL1WriteTtlMinutes(), TimeUnit.MINUTES);
        } catch (Exception ignored) {
        }
    }

    public String buildListKey(Long categoryId, Long tagId, Integer page, Integer size, String keyword) {
        String raw = (categoryId == null ? "0" : categoryId) + ":"
                + (tagId == null ? "0" : tagId) + ":"
                + page + ":" + size + ":"
                + (StringUtils.hasText(keyword) ? keyword : "");
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    public boolean shouldCacheList(Integer page, String keyword) {
        return page != null && page <= properties.getListCacheMaxPage()
                && !StringUtils.hasText(keyword);
    }

    public boolean isDetailNullCached(Long id, String lang) {
        String raw = redis.opsForValue().get(DETAIL_PREFIX + detailKey(id, lang));
        return NULL_MARKER.equals(raw);
    }

    public void evictArticle(Long articleId) {
        localCacheManager.evictAll(L1_DETAIL);
        localCacheManager.evictAll(L1_LIST);
        redis.delete(redis.keys(DETAIL_PREFIX + "*"));
        redis.delete(redis.keys(LIST_PREFIX + "*"));
    }

    public void delayDoubleDelete(Long articleId) {
        evictArticle(articleId);
        try {
            Thread.sleep(properties.getEvictDelayMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        evictArticle(articleId);
    }

    private static String detailKey(Long id, String lang) {
        return id + ":" + (StringUtils.hasText(lang) ? lang : "zh");
    }
}
