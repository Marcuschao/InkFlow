package com.blog.personalblogbackend.service;

import com.blog.personalblogbackend.model.vo.chat.OnlineUserVo;

import java.util.List;

public interface ChatOnlineService {
    void markOnline(String sessionId, Long userId, String username, String avatar, boolean admin);

    void markOffline(String sessionId);

    void markOfflineByUserId(Long userId);

    List<OnlineUserVo> listOnlineUsers();
}
