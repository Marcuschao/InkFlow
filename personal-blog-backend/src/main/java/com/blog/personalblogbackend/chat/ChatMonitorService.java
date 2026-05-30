package com.blog.personalblogbackend.chat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.personalblogbackend.config.websocket.ChatProperties;
import com.blog.personalblogbackend.mapper.ChatFailedQueueMapper;
import com.blog.personalblogbackend.model.entity.ChatFailedQueue;
import com.blog.personalblogbackend.service.ChatOnlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatMonitorService {

    private static final String OFFLINE_PREFIX = "offline:msg:";
    private static final String FAILED_PENDING = "PENDING";

    private final ChatOnlineService chatOnlineService;
    private final ChatFailedQueueMapper chatFailedQueueMapper;
    private final StringRedisTemplate redis;
    private final ChatProperties chatProperties;

    public Map<String, Object> snapshot() {
        Map<String, Object> data = new HashMap<>();
        var sessions = chatOnlineService.listOnlineSessions();
        data.put("onlineSessionCount", sessions.size());
        data.put("onlineUserCount", chatOnlineService.listOnlineUsers().size());
        data.put("offlineQueueLength", sumOfflineQueueLength());
        data.put("failedQueuePending", chatFailedQueueMapper.selectCount(new LambdaQueryWrapper<ChatFailedQueue>()
                .eq(ChatFailedQueue::getStatus, FAILED_PENDING)));
        data.put("streamLag", pendingStreamCount());
        return data;
    }

    private long sumOfflineQueueLength() {
        Set<String> keys = redis.keys(OFFLINE_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        long total = 0;
        for (String key : keys) {
            Long len = redis.opsForList().size(key);
            if (len != null) {
                total += len;
            }
        }
        return total;
    }

    private long pendingStreamCount() {
        if (!chatProperties.isFanoutEnabled()) {
            return 0;
        }
        try {
            PendingMessagesSummary summary = redis.opsForStream().pending(
                    chatProperties.getFanoutStreamKey(), chatProperties.getFanoutGroup());
            if (summary == null) {
                return 0;
            }
            return summary.getTotalPendingMessages();
        } catch (Exception e) {
            return 0;
        }
    }
}
