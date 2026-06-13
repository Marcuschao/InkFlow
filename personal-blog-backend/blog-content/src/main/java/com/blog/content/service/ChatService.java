package com.blog.content.service;

import com.blog.content.model.dto.chat.ChatHistoryResult;
import com.blog.content.model.dto.chat.ChatSendRequest;
import com.blog.content.model.vo.chat.ChatMessageVo;

public interface ChatService {
    ChatHistoryResult loadHistory(Long cursor, Long afterId, Integer limit);

    ChatMessageVo send(Long userId, String username, String avatar, boolean admin, ChatSendRequest request);
}
