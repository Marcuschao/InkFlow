package com.blog.content.chat;

import com.blog.content.config.websocket.ChatProperties;
import com.blog.content.model.vo.chat.ChatMessageVo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatFanoutPublisher {

    private final StringRedisTemplate redis;
    private final ChatProperties chatProperties;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public void publish(ChatMessageVo vo) {
        if (vo == null) {
            return;
        }
        if (!chatProperties.isFanoutEnabled()) {
            messagingTemplate.convertAndSend("/topic/chat", vo);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(vo);
            var record = StreamRecords.string(Collections.singletonMap("payload", json))
                    .withStreamKey(chatProperties.getFanoutStreamKey());
            redis.opsForStream().add(record,
                    RedisStreamCommands.XAddOptions.maxlen(chatProperties.getFanoutMaxLen()));
        } catch (Exception ex) {
            log.warn("chat fanout stream publish failed messageId={}", vo.getId(), ex);
            messagingTemplate.convertAndSend("/topic/chat", vo);
        }
    }
}
