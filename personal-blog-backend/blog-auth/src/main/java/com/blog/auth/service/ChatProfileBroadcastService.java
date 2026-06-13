package com.blog.auth.service;

public interface ChatProfileBroadcastService {
    void broadcastProfileUpdate(Long userId, String nickname, String avatar);
}
