package com.blog.content.controller;

import com.blog.content.config.websocket.StompSessionAttributes;
import com.blog.content.model.dto.chat.ChatSendRequest;
import com.blog.content.model.vo.chat.ChatUserDisplayVo;
import com.blog.content.service.ChatRecallService;
import com.blog.content.service.ChatService;
import com.blog.content.service.ChatUserDisplayService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final ChatRecallService chatRecallService;
    private final ChatUserDisplayService chatUserDisplayService;

    @MessageMapping("/chat")
    public void send(ChatSendRequest request, SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        Map<String, Object> attrs = headerAccessor.getSessionAttributes();
        Long userId = attrs != null ? (Long) attrs.get(StompSessionAttributes.USER_ID) : null;
        String role = attrs != null ? (String) attrs.get(StompSessionAttributes.ROLE) : null;
        if (principal == null || userId == null) {
            throw new IllegalStateException("未登录");
        }
        ChatUserDisplayVo display = chatUserDisplayService.mapDisplayByUserIds(List.of(userId)).get(userId);
        String username = display != null ? display.getUsername()
                : (String) attrs.get(StompSessionAttributes.USERNAME);
        String avatar = display != null ? display.getAvatar()
                : (String) attrs.get(StompSessionAttributes.AVATAR);
        boolean admin = "ADMIN".equals(role);
        chatService.send(userId, username, avatar, admin, request);
    }

    @MessageMapping("/chat/recall")
    public void recall(Map<String, Long> body, SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        Map<String, Object> attrs = headerAccessor.getSessionAttributes();
        Long userId = attrs != null ? (Long) attrs.get(StompSessionAttributes.USER_ID) : null;
        String role = attrs != null ? (String) attrs.get(StompSessionAttributes.ROLE) : null;
        if (principal == null || userId == null || body == null || body.get("messageId") == null) {
            throw new IllegalStateException("未登录");
        }
        chatRecallService.recall(body.get("messageId"), userId, "ADMIN".equals(role));
    }
}
