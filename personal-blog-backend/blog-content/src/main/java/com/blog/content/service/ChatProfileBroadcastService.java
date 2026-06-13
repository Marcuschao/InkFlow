package com.blog.content.service;

public interface ChatProfileBroadcastService {

    void broadcastProfileUpdate(Long userId, String username, String avatar);
}
