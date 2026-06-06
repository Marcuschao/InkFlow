package com.blog.personalblogbackend.chat;

import com.blog.personalblogbackend.config.websocket.ChatProperties;
import com.blog.personalblogbackend.model.vo.chat.ChatMessageVo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "blog.chat.fanout-enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ChatFanoutListener {

    private final StringRedisTemplate redis;
    private final ChatProperties chatProperties;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    private String consumerName;

    @PostConstruct
    public void initGroup() {
        if (!chatProperties.isFanoutEnabled()) {
            return;
        }
        consumerName = resolveConsumerName();
        String stream = chatProperties.getFanoutStreamKey();
        String group = chatProperties.getFanoutGroup();
        try {
            redis.opsForStream().createGroup(stream, ReadOffset.from("0"), group);
        } catch (Exception ex) {
            log.debug("chat fanout group exists or stream missing: {}", ex.getMessage());
        }
    }

    @Scheduled(fixedDelay = 100)
    public void poll() {
        if (!chatProperties.isFanoutEnabled()) {
            return;
        }
        String stream = chatProperties.getFanoutStreamKey();
        String group = chatProperties.getFanoutGroup();
        Consumer consumer = Consumer.from(group, consumerName);
        List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
                consumer,
                StreamReadOptions.empty().count(20).block(Duration.ofMillis(50)),
                StreamOffset.create(stream, ReadOffset.lastConsumed()));
        if (records == null || records.isEmpty()) {
            return;
        }
        for (MapRecord<String, Object, Object> record : records) {
            try {
                Object payload = record.getValue().get("payload");
                if (payload == null) {
                    continue;
                }
                ChatMessageVo vo = objectMapper.readValue(payload.toString(), ChatMessageVo.class);
                messagingTemplate.convertAndSend("/topic/chat", vo);
                redis.opsForStream().acknowledge(stream, group, record.getId());
            } catch (Exception ex) {
                log.warn("chat fanout consume failed id={}", record.getId(), ex);
            }
        }
    }

    private static String resolveConsumerName() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID().toString().substring(0, 8);
        } catch (Exception e) {
            return "chat-consumer-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
}
