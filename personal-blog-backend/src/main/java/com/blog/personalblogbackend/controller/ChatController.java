package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.common.support.Result;
import com.blog.personalblogbackend.config.security.CurrentUserService;
import com.blog.personalblogbackend.model.dto.chat.ChatHistoryResult;
import com.blog.personalblogbackend.model.dto.chat.ChatSendRequest;
import com.blog.personalblogbackend.model.entity.User;
import com.blog.personalblogbackend.model.vo.chat.ChatMessageVo;
import com.blog.personalblogbackend.model.vo.chat.ChatUserDisplayVo;
import com.blog.personalblogbackend.model.vo.chat.OnlineUserVo;
import com.blog.personalblogbackend.service.ChatOnlineService;
import com.blog.personalblogbackend.service.ChatRecallService;
import com.blog.personalblogbackend.service.ChatReliabilityService;
import com.blog.personalblogbackend.service.ChatService;
import com.blog.personalblogbackend.service.ChatUserDisplayService;
import com.blog.personalblogbackend.service.UserMuteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatOnlineService chatOnlineService;
    private final ChatRecallService chatRecallService;
    private final ChatReliabilityService chatReliabilityService;
    private final UserMuteService userMuteService;
    private final CurrentUserService currentUserService;
    private final ChatUserDisplayService chatUserDisplayService;

    @GetMapping("/history")
    public Result<ChatHistoryResult> history(
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Long afterId,
            @RequestParam(required = false) Integer limit) {
        return Result.success(chatService.loadHistory(cursor, afterId, limit));
    }

    @GetMapping("/online-users")
    public Result<List<OnlineUserVo>> onlineUsers() {
        return Result.success(chatOnlineService.listOnlineUsers());
    }

    @PostMapping("/send")
    public Result<ChatMessageVo> send(@RequestBody ChatSendRequest request) {
        User user = currentUserService.requireUser();
        ChatUserDisplayVo display = resolveDisplay(user.getId());
        String username = display != null ? display.getUsername() : user.getUsername();
        String avatar = display != null ? display.getAvatar() : null;
        boolean admin = currentUserService.isAdmin();
        ChatMessageVo vo = chatService.send(user.getId(), username, avatar, admin, request);
        touchOnline(user, username, avatar, admin);
        return Result.success(vo);
    }

    @PostMapping("/presence")
    public Result<Void> presence() {
        User user = currentUserService.requireUser();
        ChatUserDisplayVo display = resolveDisplay(user.getId());
        String username = display != null ? display.getUsername() : user.getUsername();
        String avatar = display != null ? display.getAvatar() : null;
        touchOnline(user, username, avatar, currentUserService.isAdmin());
        return Result.success(null);
    }

    @PostMapping("/offline")
    public Result<Void> offline() {
        chatOnlineService.markOfflineByUserId(currentUserService.requireUserId());
        return Result.success(null);
    }

    @PostMapping("/recall/{id}")
    public Result<Void> recall(@PathVariable Long id) {
        chatRecallService.recall(id, currentUserService.requireUserId(), currentUserService.isAdmin());
        return Result.success(null);
    }

    @GetMapping("/mute-status")
    public Result<Map<String, Object>> muteStatus() {
        Long userId = currentUserService.requireUserId();
        Map<String, Object> data = new HashMap<>();
        data.put("muted", userMuteService.isMuted(userId));
        data.put("muteUntil", userMuteService.getMuteUntil(userId));
        return Result.success(data);
    }

    private void touchOnline(User user, String username, String avatar, boolean admin) {
        chatOnlineService.markOnline("http:" + user.getId(), user.getId(), username, avatar, admin);
        chatReliabilityService.trackPresence(user.getId());
    }

    private ChatUserDisplayVo resolveDisplay(Long userId) {
        return chatUserDisplayService.mapDisplayByUserIds(List.of(userId)).get(userId);
    }
}
