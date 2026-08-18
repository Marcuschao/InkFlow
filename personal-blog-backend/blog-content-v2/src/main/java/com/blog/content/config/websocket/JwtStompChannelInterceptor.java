package com.blog.content.config.websocket;

import com.blog.common.security.JwtUtils;
import com.blog.content.model.entity.UserProfile;
import com.blog.content.service.ChatOnlineService;
import com.blog.content.service.UserProfileLookupService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Component
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    private final JwtUtils jwtUtils;
    private final UserProfileLookupService userService;
    private final ChatProperties chatProperties;
    private final ChatOnlineService chatOnlineService;

    public JwtStompChannelInterceptor(
            JwtUtils jwtUtils,
            UserProfileLookupService userService,
            ChatProperties chatProperties,
            ChatOnlineService chatOnlineService) {
        this.jwtUtils = jwtUtils;
        this.userService = userService;
        this.chatProperties = chatProperties;
        this.chatOnlineService = chatOnlineService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        } else if (StompCommand.SEND.equals(accessor.getCommand())) {
            if (chatProperties.isGuestReadonly()
                    && isChatSendDestination(accessor.getDestination())
                    && accessor.getUser() == null) {
                throw new IllegalStateException("未登录，无法发送聊天消息");
            }
        }

        if (accessor.getUser() != null
                && !StompCommand.CONNECT.equals(accessor.getCommand())
                && !StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            refreshOnline(accessor);
        }

        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String token = resolveToken(accessor);
        if (!StringUtils.hasText(token)) {
            return;
        }
        try {
            Claims claims = jwtUtils.parseToken(token);
            Long userId = readUserId(claims);
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);
            if (userId == null || !StringUtils.hasText(username)) {
                return;
            }
            UserDisplay display = resolveDisplay(userId, username);
            accessor.setUser(new StompPrincipal(String.valueOf(userId)));
            Map<String, Object> attrs = accessor.getSessionAttributes();
            if (attrs != null) {
                attrs.put(StompSessionAttributes.USER_ID, userId);
                attrs.put(StompSessionAttributes.USERNAME, display.username());
                attrs.put(StompSessionAttributes.ROLE, role);
                attrs.put(StompSessionAttributes.AVATAR, display.avatar());
            }
            chatOnlineService.markOnline(
                    accessor.getSessionId(),
                    userId,
                    display.username(),
                    display.avatar(),
                    "ADMIN".equals(role),
                    resolveIp(accessor));
        } catch (JwtException ex) {
        }
    }

    private void refreshOnline(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            return;
        }
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null) {
            return;
        }
        Long userId = (Long) attrs.get(StompSessionAttributes.USER_ID);
        String fallbackUsername = (String) attrs.get(StompSessionAttributes.USERNAME);
        String role = (String) attrs.get(StompSessionAttributes.ROLE);
        if (userId == null) {
            return;
        }
        UserDisplay display = resolveDisplay(userId, fallbackUsername);
        attrs.put(StompSessionAttributes.USERNAME, display.username());
        attrs.put(StompSessionAttributes.AVATAR, display.avatar());
        chatOnlineService.markOnline(
                accessor.getSessionId(),
                userId,
                display.username(),
                display.avatar(),
                "ADMIN".equals(role),
                resolveIp(accessor));
    }

    private UserDisplay resolveDisplay(Long userId, String fallbackUsername) {
        Map<Long, UserProfile> profiles = userService.mapProfilesByUserIds(List.of(userId));
        UserProfile profile = profiles.get(userId);
        String username = profile != null && StringUtils.hasText(profile.getNickname())
                ? profile.getNickname()
                : fallbackUsername;
        String avatar = profile != null ? profile.getAvatar() : null;
        return new UserDisplay(username, avatar);
    }

    private record UserDisplay(String username, String avatar) {
    }

    private Long readUserId(Claims claims) {
        Object raw = claims.get("userId");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return claims.get("userId", Long.class);
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String bearer = authHeaders.get(0);
            if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
                return bearer.substring(7);
            }
        }
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }
        return null;
    }

    private boolean isChatSendDestination(String destination) {
        return destination != null && ("/app/chat".equals(destination) || destination.endsWith("/chat"));
    }

    private String resolveIp(StompHeaderAccessor accessor) {
        List<String> forwarded = accessor.getNativeHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty() && StringUtils.hasText(forwarded.get(0))) {
            return forwarded.get(0).split(",")[0].trim();
        }
        return null;
    }
}
