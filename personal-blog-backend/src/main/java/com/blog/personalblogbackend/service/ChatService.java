package com.blog.personalblogbackend.service;

import com.blog.personalblogbackend.model.dto.chat.ChatHistoryResult;
import com.blog.personalblogbackend.model.dto.chat.ChatSendRequest;
import com.blog.personalblogbackend.model.vo.chat.ChatMessageVo;

public interface ChatService {
    ChatHistoryResult loadHistory(Long cursor, Long afterId, Integer limit);

    ChatMessageVo send(Long userId, String username, String avatar, boolean admin, ChatSendRequest request);
}
