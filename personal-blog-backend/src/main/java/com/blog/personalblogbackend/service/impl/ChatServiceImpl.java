package com.blog.personalblogbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.config.websocket.ChatProperties;
import com.blog.personalblogbackend.mapper.ChatMessageMapper;
import com.blog.personalblogbackend.model.dto.chat.ChatSendRequest;
import com.blog.personalblogbackend.model.entity.ChatMessage;
import com.blog.personalblogbackend.model.vo.chat.ChatMessageVo;
import com.blog.personalblogbackend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int MAX_CONTENT_LENGTH = 1000;

    private final ChatMessageMapper chatMessageMapper;
    private final ChatProperties chatProperties;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public List<ChatMessageVo> recentHistory() {
        int limit = Math.max(1, chatProperties.getHistoryLimit());
        List<ChatMessage> rows = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .orderByDesc(ChatMessage::getCreateTime)
                .last("LIMIT " + limit));
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        Collections.reverse(rows);
        return rows.stream().map(this::toVo).collect(Collectors.toList());
    }

    @Override
    public ChatMessageVo send(Long userId, String username, String avatar, boolean admin, ChatSendRequest request) {
        if (userId == null || !StringUtils.hasText(username)) {
            throw new ServiceException(401, "未登录");
        }
        if (request == null || !StringUtils.hasText(request.getContent())) {
            throw new ServiceException(400, "消息内容不能为空");
        }
        String content = request.getContent().trim();
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new ServiceException(400, "消息内容过长");
        }
        ChatMessage message = new ChatMessage();
        message.setUserId(userId);
        message.setUsername(username);
        message.setAvatar(avatar);
        message.setContent(content);
        message.setIsAdmin(admin ? 1 : 0);
        message.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(message);
        ChatMessageVo vo = toVo(message);
        messagingTemplate.convertAndSend("/topic/chat", vo);
        return vo;
    }

    private ChatMessageVo toVo(ChatMessage message) {
        ChatMessageVo vo = new ChatMessageVo();
        vo.setId(message.getId());
        vo.setUserId(message.getUserId());
        vo.setUsername(message.getUsername());
        vo.setAvatar(message.getAvatar());
        vo.setContent(message.getContent());
        vo.setAdmin(message.getIsAdmin() != null && message.getIsAdmin() == 1);
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
