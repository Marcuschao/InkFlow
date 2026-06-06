package com.blog.personalblogbackend.service.impl;

import com.blog.personalblogbackend.model.vo.chat.ChatProfileUpdateEvent;
import com.blog.personalblogbackend.service.ChatOnlineService;
import com.blog.personalblogbackend.service.ChatProfileBroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatProfileBroadcastServiceImpl implements ChatProfileBroadcastService {

    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;
    private final ChatOnlineService chatOnlineService;

    @Override
    public void broadcastProfileUpdate(Long userId, String username, String avatar) {
        if (userId == null) {
            return;
        }
        ChatProfileUpdateEvent event = new ChatProfileUpdateEvent();
        event.setUserId(userId);
        event.setUsername(username);
        event.setAvatar(avatar);
        SimpMessagingTemplate messagingTemplate = messagingTemplateProvider.getIfAvailable();
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/chat/profile", event);
        }
        chatOnlineService.refreshUserDisplay(userId, username, avatar);
    }
}
