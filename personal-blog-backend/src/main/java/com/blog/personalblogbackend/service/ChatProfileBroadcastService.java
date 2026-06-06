package com.blog.personalblogbackend.service;

public interface ChatProfileBroadcastService {

    void broadcastProfileUpdate(Long userId, String username, String avatar);
}
