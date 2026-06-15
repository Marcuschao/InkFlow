package com.blog.content.knowledge.cache;

import com.blog.content.config.properties.KnowledgeProperties;
import com.blog.content.model.vo.knowledge.KnowledgeGraphVo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class KnowledgeGraphCacheService {
    private static final String GRAPH_KEY = "knowledge:graph:v1";
    private static final String HOT_TAGS_KEY = "hot:tags";

    private final StringRedisTemplate redis;
    private final KnowledgeProperties properties;
    private final ObjectMapper objectMapper;

    public KnowledgeGraphCacheService(StringRedisTemplate redis,
                                        KnowledgeProperties properties,
                                        ObjectMapper objectMapper) {
        this.redis = redis;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public KnowledgeGraphVo getGraph() {
        String raw = redis.opsForValue().get(GRAPH_KEY);
        if (raw == null) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, KnowledgeGraphVo.class);
        } catch (Exception e) {
            redis.delete(GRAPH_KEY);
            return null;
        }
    }

    public void putGraph(KnowledgeGraphVo graph) {
        try {
            redis.opsForValue().set(GRAPH_KEY, objectMapper.writeValueAsString(graph),
                    properties.getGraphCacheTtlHours(), TimeUnit.HOURS);
        } catch (Exception ignored) {
        }
    }

    public void evictGraph() {
        redis.delete(GRAPH_KEY);
    }

    public void putHotTag(Long tagId, double score) {
        redis.opsForZSet().add(HOT_TAGS_KEY, String.valueOf(tagId), score);
    }

    public void replaceHotTags(List<ZSetOperations.TypedTuple<String>> tuples) {
        redis.delete(HOT_TAGS_KEY);
        if (tuples != null && !tuples.isEmpty()) {
            redis.opsForZSet().add(HOT_TAGS_KEY, new java.util.HashSet<>(tuples));
        }
    }

    public List<Long> getHotTagIds(int limit) {
        Set<String> ids = redis.opsForZSet().reverseRange(HOT_TAGS_KEY, 0, Math.max(0, limit - 1));
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (String id : ids) {
            try {
                result.add(Long.parseLong(id));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    public void incrementSearchTag(Long tagId) {
        if (tagId == null) {
            return;
        }
        redis.opsForValue().increment("search:tag:" + tagId);
    }

    public double getSearchTagScore(Long tagId) {
        String v = redis.opsForValue().get("search:tag:" + tagId);
        if (v == null) {
            return 0;
        }
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
