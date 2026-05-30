package com.blog.personalblogbackend.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.personalblogbackend.mapper.ChatFailedQueueMapper;
import com.blog.personalblogbackend.mapper.ChatMessageMapper;
import com.blog.personalblogbackend.model.entity.ChatFailedQueue;
import com.blog.personalblogbackend.model.entity.ChatMessage;
import com.blog.personalblogbackend.model.vo.chat.ChatAckVo;
import com.blog.personalblogbackend.model.vo.chat.ChatMessageVo;
import com.blog.personalblogbackend.chat.ChatFanoutPublisher;
import com.blog.personalblogbackend.service.impl.ChatReliabilityServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatFailedQueueScheduler {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_FAILED = "FAILED";
    private static final int MAX_RETRY = 5;

    private final ChatFailedQueueMapper chatFailedQueueMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatFanoutPublisher chatFanoutPublisher;

    @Scheduled(fixedDelay = 30000)
    public void retryFailed() {
        List<ChatFailedQueue> rows = chatFailedQueueMapper.selectList(new LambdaQueryWrapper<ChatFailedQueue>()
                .eq(ChatFailedQueue::getStatus, STATUS_PENDING)
                .orderByAsc(ChatFailedQueue::getId)
                .last("LIMIT 50"));
        for (ChatFailedQueue row : rows) {
            if (row.getRetryCount() != null && row.getRetryCount() >= MAX_RETRY) {
                row.setStatus(STATUS_FAILED);
                chatFailedQueueMapper.updateById(row);
                log.error("chat failed queue exhausted id={} messageId={}", row.getId(), row.getMessageId());
                continue;
            }
            ChatMessage message = chatMessageMapper.selectById(row.getMessageId());
            if (message == null) {
                row.setStatus(STATUS_FAILED);
                chatFailedQueueMapper.updateById(row);
                continue;
            }
            ChatMessageVo vo = ChatReliabilityServiceImpl.toVo(message, null);
            try {
                chatFanoutPublisher.publish(vo);
                ChatAckVo ack = new ChatAckVo();
                ack.setMessageId(vo.getId());
                ack.setStatus("OK");
                messagingTemplate.convertAndSendToUser(String.valueOf(row.getUserId()), "/queue/chat/ack", ack);
                chatFailedQueueMapper.deleteById(row.getId());
            } catch (Exception ex) {
                row.setRetryCount((row.getRetryCount() == null ? 0 : row.getRetryCount()) + 1);
                if (row.getRetryCount() >= MAX_RETRY) {
                    row.setStatus(STATUS_FAILED);
                }
                chatFailedQueueMapper.updateById(row);
                log.warn("chat retry failed id={}", row.getId(), ex);
            }
        }
    }
}
