package com.blog.content.service.impl;

import com.blog.content.common.exception.ServiceException;
import com.blog.content.mapper.ChatMessageMapper;
import com.blog.content.model.entity.ChatMessage;
import com.blog.content.model.vo.chat.ChatRecallEvent;
import com.blog.content.service.ChatRecallService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ChatRecallServiceImpl implements ChatRecallService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Duration USER_RECALL_LIMIT = Duration.ofMinutes(2);

    private final ChatMessageMapper chatMessageMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void recall(Long messageId, Long operatorId, boolean admin) {
        if (messageId == null || operatorId == null) {
            throw new ServiceException(400, "参数无效");
        }
        ChatMessage message = chatMessageMapper.selectById(messageId);
        if (message == null) {
            throw new ServiceException(404, "消息不存在");
        }
        if (message.getRecalled() != null && message.getRecalled() == 1) {
            return;
        }
        if (!admin) {
            if (message.getUserId() == null || !message.getUserId().equals(operatorId)) {
                throw new ServiceException(403, "无权撤回该消息");
            }
            if (message.getCreateTime() == null
                    || message.getCreateTime().isBefore(LocalDateTime.now(ZONE).minus(USER_RECALL_LIMIT))) {
                throw new ServiceException(400, "已超过撤回时限");
            }
        }
        ChatMessage patch = new ChatMessage();
        patch.setId(messageId);
        patch.setRecalled(1);
        chatMessageMapper.updateById(patch);

        ChatRecallEvent event = new ChatRecallEvent();
        event.setMessageId(messageId);
        event.setRecalledBy(operatorId);
        event.setAdmin(admin);
        messagingTemplate.convertAndSend("/topic/chat/recall", event);
    }
}
