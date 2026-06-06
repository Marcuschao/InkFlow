package com.blog.personalblogbackend.service.impl;

import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.mapper.ChatFailedQueueMapper;
import com.blog.personalblogbackend.mapper.ChatMessageMapper;
import com.blog.personalblogbackend.model.dto.chat.ChatSendRequest;
import com.blog.personalblogbackend.model.entity.ChatFailedQueue;
import com.blog.personalblogbackend.model.entity.ChatMessage;
import com.blog.personalblogbackend.model.vo.chat.ChatAckVo;
import com.blog.personalblogbackend.model.vo.chat.ChatMessageVo;
import com.blog.personalblogbackend.chat.ChatFanoutPublisher;
import com.blog.personalblogbackend.service.ChatOnlineService;
import com.blog.personalblogbackend.service.ChatReliabilityService;
import com.blog.personalblogbackend.service.ChatUserDisplayService;
import com.blog.personalblogbackend.service.SensitiveWordService;
import com.blog.personalblogbackend.service.UserMuteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatReliabilityServiceImpl implements ChatReliabilityService {

    private static final int MAX_CONTENT_LENGTH = 1000;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Duration DEDUP_TTL = Duration.ofMinutes(5);
    private static final Duration OFFLINE_TTL = Duration.ofHours(24);
    private static final String DEDUP_PREFIX = "processed:msg:";
    private static final String DEDUP_RESULT_PREFIX = "dedup:msg:";
    private static final String OFFLINE_PREFIX = "offline:msg:";
    private static final String SUBSCRIBED_USERS = "chat:subscribed:users";
    private static final String FAILED_STATUS_PENDING = "PENDING";
    private static final String ACK_OK = "OK";

    private final ChatMessageMapper chatMessageMapper;
    private final ChatFailedQueueMapper chatFailedQueueMapper;
    private final SensitiveWordService sensitiveWordService;
    private final UserMuteService userMuteService;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final ChatOnlineService chatOnlineService;
    private final ChatFanoutPublisher chatFanoutPublisher;
    private final ChatUserDisplayService chatUserDisplayService;

    @Override
    public ChatMessageVo send(Long userId, String username, String avatar, boolean admin, ChatSendRequest request) {
        if (userId == null || !StringUtils.hasText(username)) {
            throw new ServiceException(401, "未登录");
        }
        if (request == null || !StringUtils.hasText(request.getContent())) {
            throw new ServiceException(400, "消息内容不能为空");
        }
        String content = sensitiveWordService.replace(request.getContent().trim());
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new ServiceException(400, "消息内容过长");
        }
        if (userMuteService.isMuted(userId)) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("muteUntil", userMuteService.getMuteUntil(userId));
            throw new ServiceException(403, "您已被禁言", payload);
        }

        String clientMsgId = StringUtils.hasText(request.getClientMsgId()) ? request.getClientMsgId().trim() : null;
        String dedupKey = buildDedupKey(userId, clientMsgId, content);
        ChatMessageVo cached = readCachedMessage(dedupKey);
        if (cached != null) {
            chatUserDisplayService.enrichMessages(List.of(cached));
            sendAck(userId, clientMsgId, cached.getId(), ACK_OK);
            return cached;
        }

        ChatMessage message = new ChatMessage();
        message.setUserId(userId);
        message.setUsername(username);
        message.setAvatar(avatar);
        message.setContent(content);
        message.setIsAdmin(admin ? 1 : 0);
        message.setRecalled(0);
        message.setCreateTime(LocalDateTime.now(ZONE));
        chatMessageMapper.insert(message);

        ChatMessageVo vo = toVo(message, clientMsgId);
        chatUserDisplayService.enrichMessages(List.of(vo));
        cacheDedup(dedupKey, vo);
        trackPresence(userId);

        try {
            chatFanoutPublisher.publish(vo);
            sendAck(userId, clientMsgId, vo.getId(), ACK_OK);
            enqueueOffline(vo, userId);
        } catch (Exception ex) {
            log.warn("chat broadcast failed messageId={}", vo.getId(), ex);
            saveFailedQueue(vo, userId, content);
        }
        return vo;
    }

    @Override
    public void trackPresence(Long userId) {
        if (userId == null) {
            return;
        }
        redis.opsForSet().add(SUBSCRIBED_USERS, String.valueOf(userId));
    }

    @Async
    @Override
    public void drainOfflineMessages(Long userId) {
        if (userId == null) {
            return;
        }
        String key = OFFLINE_PREFIX + userId;
        while (true) {
            String json = redis.opsForList().rightPop(key);
            if (!StringUtils.hasText(json)) {
                break;
            }
            try {
                ChatMessageVo vo = objectMapper.readValue(json, ChatMessageVo.class);
                messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/chat/offline", vo);
            } catch (Exception ex) {
                log.warn("drain offline failed userId={}", userId, ex);
                break;
            }
        }
    }

    private String buildDedupKey(Long userId, String clientMsgId, String content) {
        if (StringUtils.hasText(clientMsgId)) {
            return userId + ":" + clientMsgId;
        }
        String hash = DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8));
        return userId + ":" + System.currentTimeMillis() / 1000 + ":" + hash;
    }

    private ChatMessageVo readCachedMessage(String dedupKey) {
        String marker = redis.opsForValue().get(DEDUP_PREFIX + dedupKey);
        if (!StringUtils.hasText(marker)) {
            return null;
        }
        String json = redis.opsForValue().get(DEDUP_RESULT_PREFIX + dedupKey);
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ChatMessageVo.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private void cacheDedup(String dedupKey, ChatMessageVo vo) {
        try {
            String json = objectMapper.writeValueAsString(vo);
            redis.opsForValue().set(DEDUP_PREFIX + dedupKey, "1", DEDUP_TTL);
            redis.opsForValue().set(DEDUP_RESULT_PREFIX + dedupKey, json, DEDUP_TTL);
        } catch (Exception ignored) {
        }
    }

    private void sendAck(Long userId, String clientMsgId, Long messageId, String status) {
        ChatAckVo ack = new ChatAckVo();
        ack.setClientMsgId(clientMsgId);
        ack.setMessageId(messageId);
        ack.setStatus(status);
        messagingTemplate.convertAndSendToUser(String.valueOf(userId), "/queue/chat/ack", ack);
    }

    private void enqueueOffline(ChatMessageVo vo, Long senderId) {
        Set<Long> wsConnected = listWsConnectedUserIds();
        Set<String> subscribed = redis.opsForSet().members(SUBSCRIBED_USERS);
        if (subscribed == null || subscribed.isEmpty()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(vo);
            for (String uidStr : subscribed) {
                Long uid = Long.valueOf(uidStr);
                if (uid.equals(senderId) || wsConnected.contains(uid)) {
                    continue;
                }
                String key = OFFLINE_PREFIX + uid;
                redis.opsForList().leftPush(key, json);
                redis.expire(key, OFFLINE_TTL);
            }
        } catch (Exception ex) {
            log.warn("enqueue offline failed messageId={}", vo.getId(), ex);
        }
    }

    private Set<Long> listWsConnectedUserIds() {
        Set<Long> ids = new HashSet<>();
        for (var user : chatOnlineService.listOnlineUsers()) {
            if (user.getSessionId() != null && !user.getSessionId().startsWith("http:")) {
                ids.add(user.getUserId());
            }
        }
        return ids;
    }

    private void saveFailedQueue(ChatMessageVo vo, Long userId, String content) {
        ChatFailedQueue row = new ChatFailedQueue();
        row.setMessageId(vo.getId());
        row.setUserId(userId);
        row.setContent(content);
        row.setRetryCount(0);
        row.setStatus(FAILED_STATUS_PENDING);
        row.setCreateTime(LocalDateTime.now(ZONE));
        chatFailedQueueMapper.insert(row);
    }

    public static ChatMessageVo toVo(ChatMessage message, String clientMsgId) {
        ChatMessageVo vo = new ChatMessageVo();
        vo.setId(message.getId());
        vo.setUserId(message.getUserId());
        vo.setUsername(message.getUsername());
        vo.setAvatar(message.getAvatar());
        vo.setContent(message.getContent());
        vo.setAdmin(message.getIsAdmin() != null && message.getIsAdmin() == 1);
        vo.setRecalled(message.getRecalled() != null && message.getRecalled() == 1);
        vo.setCreateTime(message.getCreateTime());
        vo.setClientMsgId(clientMsgId);
        return vo;
    }
}
