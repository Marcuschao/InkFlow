package com.blog.personalblogbackend.service.impl;

import com.blog.personalblogbackend.config.websocket.ChatProperties;
import com.blog.personalblogbackend.model.vo.chat.OnlineUserVo;
import com.blog.personalblogbackend.service.ChatOnlineService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatOnlineServiceImpl implements ChatOnlineService {

    private static final String KEY_PREFIX = "chat:online:session:";

    private final StringRedisTemplate redis;
    private final ChatProperties chatProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void markOnline(String sessionId, Long userId, String username, String avatar, boolean admin) {
        if (!StringUtils.hasText(sessionId) || userId == null || username == null) {
            return;
        }
        OnlineUserVo vo = new OnlineUserVo();
        vo.setUserId(userId);
        vo.setUsername(username);
        vo.setAvatar(avatar);
        vo.setAdmin(admin);
        try {
            String json = objectMapper.writeValueAsString(vo);
            redis.opsForValue().set(key(sessionId), json, Duration.ofSeconds(chatProperties.getOnlineTtlSeconds()));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void markOffline(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }
        redis.delete(key(sessionId));
    }

    @Override
    public void markOfflineByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        redis.delete(key("http:" + userId));
        Set<String> keys = redis.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String redisKey : keys) {
            String json = redis.opsForValue().get(redisKey);
            if (json == null) {
                continue;
            }
            try {
                OnlineUserVo vo = objectMapper.readValue(json, new TypeReference<>() {
                });
                if (vo != null && userId.equals(vo.getUserId())) {
                    redis.delete(redisKey);
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public List<OnlineUserVo> listOnlineUsers() {
        Set<String> keys = redis.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        Map<Long, OnlineUserVo> dedup = new LinkedHashMap<>();
        for (String redisKey : keys) {
            String json = redis.opsForValue().get(redisKey);
            if (json == null) {
                continue;
            }
            try {
                OnlineUserVo vo = objectMapper.readValue(json, new TypeReference<>() {
                });
                if (vo != null && vo.getUserId() != null) {
                    dedup.put(vo.getUserId(), vo);
                }
            } catch (Exception ignored) {
            }
        }
        List<OnlineUserVo> users = new ArrayList<>(dedup.values());
        users.sort(Comparator.comparing(OnlineUserVo::getUsername, Comparator.nullsLast(String::compareToIgnoreCase)));
        return users;
    }

    private String key(String sessionId) {
        return KEY_PREFIX + sessionId;
    }
}
