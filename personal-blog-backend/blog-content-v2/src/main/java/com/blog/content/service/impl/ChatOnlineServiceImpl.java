package com.blog.content.service.impl;

import com.blog.content.config.websocket.ChatProperties;
import com.blog.content.model.vo.chat.OnlineUserVo;
import com.blog.content.service.ChatOnlineService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatOnlineServiceImpl implements ChatOnlineService {

    private static final String ONLINE_ZSET = "chat:online:z";
    private static final String META_PREFIX = "chat:online:meta:";
    private static final String LEGACY_PREFIX = "chat:online:session:";
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final StringRedisTemplate redis;
    private final ChatProperties chatProperties;
    private final ObjectMapper objectMapper;

    private ObjectMapper redisMapper() {
        return objectMapper.copy().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void markOnline(String sessionId, Long userId, String username, String avatar, boolean admin) {
        markOnline(sessionId, userId, username, avatar, admin, null);
    }

    @Override
    public void markOnline(String sessionId, Long userId, String username, String avatar, boolean admin, String ip) {
        if (!StringUtils.hasText(sessionId) || userId == null || username == null) {
            return;
        }
        if (isHttpSession(sessionId)) {
            if (hasActiveWsSession(userId)) {
                return;
            }
        } else {
            removeLegacyHttpKey(userId);
        }
        OnlineUserVo existing = readMeta(sessionId);
        OnlineUserVo vo = new OnlineUserVo();
        vo.setUserId(userId);
        vo.setUsername(username);
        vo.setAvatar(avatar);
        vo.setAdmin(admin);
        vo.setSessionId(sessionId);
        vo.setIp(StringUtils.hasText(ip) ? ip : existing != null ? existing.getIp() : null);
        vo.setOnlineAt(existing != null && existing.getOnlineAt() != null ? existing.getOnlineAt() : LocalDateTime.now(ZONE));
        long score = System.currentTimeMillis();
        try {
            String json = redisMapper().writeValueAsString(vo);
            redis.opsForZSet().add(ONLINE_ZSET, sessionId, score);
            redis.opsForValue().set(metaKey(sessionId), json, Duration.ofSeconds(chatProperties.getOnlineTtlSeconds()));
            removeLegacyKey(sessionId);
        } catch (Exception ex) {
            log.warn("markOnline failed sessionId={} userId={}", sessionId, userId, ex);
        }
    }

    @Override
    public void markOffline(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }
        redis.opsForZSet().remove(ONLINE_ZSET, sessionId);
        redis.delete(metaKey(sessionId));
        removeLegacyKey(sessionId);
    }

    @Override
    public void markOfflineByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        markOffline("http:" + userId);
        removeLegacyHttpKey(userId);
        for (OnlineUserVo vo : listOnlineSessions()) {
            if (userId.equals(vo.getUserId())) {
                markOffline(vo.getSessionId());
            }
        }
    }

    @Override
    public List<OnlineUserVo> listOnlineUsers() {
        List<OnlineUserVo> sessions = listOnlineSessions();
        Map<Long, OnlineUserVo> dedup = new LinkedHashMap<>();
        for (OnlineUserVo vo : sessions) {
            if (vo != null && vo.getUserId() != null) {
                putOnlineUser(dedup, vo);
            }
        }
        List<OnlineUserVo> users = new ArrayList<>(dedup.values());
        users.sort(Comparator.comparing(OnlineUserVo::getUsername, Comparator.nullsLast(String::compareToIgnoreCase)));
        return users;
    }

    @Override
    public List<OnlineUserVo> listOnlineSessions() {
        long minScore = System.currentTimeMillis() - chatProperties.getOnlineTtlSeconds() * 1000L;
        Set<String> sessionIds = redis.opsForZSet().rangeByScore(ONLINE_ZSET, minScore, Double.MAX_VALUE);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return migrateLegacySessions();
        }
        List<OnlineUserVo> sessions = new ArrayList<>();
        for (String sessionId : sessionIds) {
            OnlineUserVo vo = readMeta(sessionId);
            if (vo != null) {
                vo.setSessionId(sessionId);
                sessions.add(vo);
            } else {
                redis.opsForZSet().remove(ONLINE_ZSET, sessionId);
            }
        }
        sessions.sort(Comparator.comparing(OnlineUserVo::getOnlineAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed());
        return filterHttpFallbackSessions(sessions);
    }

    private List<OnlineUserVo> migrateLegacySessions() {
        Set<String> keys = redis.keys(LEGACY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        List<OnlineUserVo> sessions = new ArrayList<>();
        long score = System.currentTimeMillis();
        Duration ttl = Duration.ofSeconds(chatProperties.getOnlineTtlSeconds());
        for (String redisKey : keys) {
            String json = redis.opsForValue().get(redisKey);
            if (json == null) {
                continue;
            }
            try {
                OnlineUserVo vo = redisMapper().readValue(json, new TypeReference<>() {
                });
                if (vo != null) {
                    String sessionId = redisKey.substring(LEGACY_PREFIX.length());
                    vo.setSessionId(sessionId);
                    redis.opsForZSet().add(ONLINE_ZSET, sessionId, score);
                    redis.opsForValue().set(metaKey(sessionId), json, ttl);
                    redis.delete(redisKey);
                    sessions.add(vo);
                }
            } catch (Exception ignored) {
            }
        }
        return filterHttpFallbackSessions(sessions);
    }

    private List<OnlineUserVo> filterHttpFallbackSessions(List<OnlineUserVo> sessions) {
        Set<Long> wsUsers = new java.util.HashSet<>();
        for (OnlineUserVo vo : sessions) {
            if (vo != null && vo.getUserId() != null && isWsSession(vo.getSessionId())) {
                wsUsers.add(vo.getUserId());
            }
        }
        if (wsUsers.isEmpty()) {
            return sessions;
        }
        List<OnlineUserVo> filtered = new ArrayList<>();
        for (OnlineUserVo vo : sessions) {
            if (vo == null) {
                continue;
            }
            if (isHttpSession(vo.getSessionId()) && wsUsers.contains(vo.getUserId())) {
                continue;
            }
            filtered.add(vo);
        }
        return filtered;
    }

    private boolean hasActiveWsSession(Long userId) {
        for (OnlineUserVo vo : listOnlineSessions()) {
            if (userId.equals(vo.getUserId()) && isWsSession(vo.getSessionId())) {
                return true;
            }
        }
        return false;
    }

    private OnlineUserVo readMeta(String sessionId) {
        String json = redis.opsForValue().get(metaKey(sessionId));
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return redisMapper().readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return null;
        }
    }

    private void putOnlineUser(Map<Long, OnlineUserVo> dedup, OnlineUserVo vo) {
        OnlineUserVo existing = dedup.get(vo.getUserId());
        if (existing == null || preferSession(vo, existing)) {
            dedup.put(vo.getUserId(), vo);
        }
    }

    private boolean preferSession(OnlineUserVo candidate, OnlineUserVo current) {
        boolean candidateWs = isWsSession(candidate.getSessionId());
        boolean currentWs = isWsSession(current.getSessionId());
        if (candidateWs && !currentWs) {
            return true;
        }
        if (!candidateWs && currentWs) {
            return false;
        }
        return existingOnlineAt(candidate).isAfter(existingOnlineAt(current));
    }

    private LocalDateTime existingOnlineAt(OnlineUserVo vo) {
        return vo.getOnlineAt() != null ? vo.getOnlineAt() : LocalDateTime.MIN;
    }

    private boolean isWsSession(String sessionId) {
        return StringUtils.hasText(sessionId) && !isHttpSession(sessionId);
    }

    private boolean isHttpSession(String sessionId) {
        return StringUtils.hasText(sessionId) && sessionId.startsWith("http:");
    }

    private void removeLegacyKey(String sessionId) {
        redis.delete(LEGACY_PREFIX + sessionId);
    }

    private void removeLegacyHttpKey(Long userId) {
        redis.delete(LEGACY_PREFIX + "http:" + userId);
    }

    private static String metaKey(String sessionId) {
        return META_PREFIX + sessionId;
    }

    @Override
    public void refreshUserDisplay(Long userId, String username, String avatar) {
        if (userId == null) {
            return;
        }
        for (OnlineUserVo session : listOnlineSessions()) {
            if (!userId.equals(session.getUserId()) || !StringUtils.hasText(session.getSessionId())) {
                continue;
            }
            session.setUsername(username);
            session.setAvatar(avatar);
            try {
                String json = redisMapper().writeValueAsString(session);
                redis.opsForValue().set(
                        metaKey(session.getSessionId()),
                        json,
                        Duration.ofSeconds(chatProperties.getOnlineTtlSeconds()));
            } catch (Exception ex) {
                log.warn("refreshUserDisplay failed userId={} sessionId={}", userId, session.getSessionId(), ex);
            }
        }
    }
}
