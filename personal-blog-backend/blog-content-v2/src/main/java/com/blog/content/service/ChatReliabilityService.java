package com.blog.content.service;

import com.blog.content.model.dto.chat.ChatSendRequest;
import com.blog.content.model.vo.chat.ChatMessageVo;

public interface ChatReliabilityService {
    ChatMessageVo send(Long userId, String username, String avatar, boolean admin, ChatSendRequest request);

    void drainOfflineMessages(Long userId);

    void trackPresence(Long userId);
}
