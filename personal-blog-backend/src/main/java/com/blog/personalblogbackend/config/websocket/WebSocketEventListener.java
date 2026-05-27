package com.blog.personalblogbackend.config.websocket;

import com.blog.personalblogbackend.service.ChatOnlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final ChatOnlineService chatOnlineService;

    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            chatOnlineService.markOffline(sessionId);
        }
    }

    @EventListener
    public void onSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null || !destination.contains("chat")) {
            return;
        }
        if (accessor.getUser() == null) {
            return;
        }
        var attrs = accessor.getSessionAttributes();
        if (attrs == null) {
            return;
        }
        Long userId = (Long) attrs.get(StompSessionAttributes.USER_ID);
        String username = (String) attrs.get(StompSessionAttributes.USERNAME);
        String avatar = (String) attrs.get(StompSessionAttributes.AVATAR);
        String role = (String) attrs.get(StompSessionAttributes.ROLE);
        if (userId == null || username == null) {
            return;
        }
        chatOnlineService.markOnline(
                accessor.getSessionId(),
                userId,
                username,
                avatar,
                "ADMIN".equals(role));
    }
}
