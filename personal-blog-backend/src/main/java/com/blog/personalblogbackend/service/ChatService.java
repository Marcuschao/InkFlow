package com.blog.personalblogbackend.service;

import com.blog.personalblogbackend.model.dto.chat.ChatSendRequest;
import com.blog.personalblogbackend.model.vo.chat.ChatMessageVo;
import com.blog.personalblogbackend.model.vo.chat.OnlineUserVo;

import java.util.List;

public interface ChatService {
    List<ChatMessageVo> recentHistory();

    ChatMessageVo send(Long userId, String username, String avatar, boolean admin, ChatSendRequest request);
}
