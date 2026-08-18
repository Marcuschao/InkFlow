package com.blog.content.service;

import com.blog.content.model.vo.chat.OnlineUserVo;

import java.util.List;

public interface ChatOnlineService {
    void markOnline(String sessionId, Long userId, String username, String avatar, boolean admin);

    void markOnline(String sessionId, Long userId, String username, String avatar, boolean admin, String ip);

    void markOffline(String sessionId);

    void markOfflineByUserId(Long userId);

    List<OnlineUserVo> listOnlineUsers();

    List<OnlineUserVo> listOnlineSessions();

    void refreshUserDisplay(Long userId, String username, String avatar);
}
