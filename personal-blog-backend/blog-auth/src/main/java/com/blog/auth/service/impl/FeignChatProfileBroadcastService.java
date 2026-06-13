package com.blog.auth.service.impl;

import com.blog.common.dto.ChatProfileBroadcastRequest;
import com.blog.common.feign.ChatFeignClient;
import com.blog.auth.service.ChatProfileBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeignChatProfileBroadcastService implements ChatProfileBroadcastService {

    private final ChatFeignClient chatFeignClient;

    @Override
    public void broadcastProfileUpdate(Long userId, String nickname, String avatar) {
        if (userId == null) {
            return;
        }
        try {
            chatFeignClient.broadcastProfile(new ChatProfileBroadcastRequest(userId, nickname, avatar));
        } catch (Exception e) {
            log.warn("聊天资料广播失败 userId={}: {}", userId, e.getMessage());
        }
    }
}
