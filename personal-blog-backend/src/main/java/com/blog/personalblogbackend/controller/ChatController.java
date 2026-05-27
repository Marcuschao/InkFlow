package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.common.support.Result;
import com.blog.personalblogbackend.config.security.CurrentUserService;
import com.blog.personalblogbackend.model.dto.chat.ChatSendRequest;
import com.blog.personalblogbackend.model.entity.User;
import com.blog.personalblogbackend.model.entity.UserProfile;
import com.blog.personalblogbackend.model.vo.chat.ChatMessageVo;
import com.blog.personalblogbackend.model.vo.chat.OnlineUserVo;
import com.blog.personalblogbackend.service.ChatOnlineService;
import com.blog.personalblogbackend.service.ChatService;
import com.blog.personalblogbackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatOnlineService chatOnlineService;
    private final CurrentUserService currentUserService;
    private final UserService userService;

    @GetMapping("/history")
    public Result<List<ChatMessageVo>> history() {
        return Result.success(chatService.recentHistory());
    }

    @GetMapping("/online-users")
    public Result<List<OnlineUserVo>> onlineUsers() {
        return Result.success(chatOnlineService.listOnlineUsers());
    }

    @PostMapping("/send")
    public Result<ChatMessageVo> send(@RequestBody ChatSendRequest request) {
        User user = currentUserService.requireUser();
        UserProfile profile = userService.mapProfilesByUserIds(List.of(user.getId())).get(user.getId());
        String avatar = profile != null ? profile.getAvatar() : null;
        boolean admin = currentUserService.isAdmin();
        ChatMessageVo vo = chatService.send(user.getId(), user.getUsername(), avatar, admin, request);
        touchOnline(user, profile, admin);
        return Result.success(vo);
    }

    @PostMapping("/presence")
    public Result<Void> presence() {
        User user = currentUserService.requireUser();
        UserProfile profile = userService.mapProfilesByUserIds(List.of(user.getId())).get(user.getId());
        touchOnline(user, profile, currentUserService.isAdmin());
        return Result.success(null);
    }

    @PostMapping("/offline")
    public Result<Void> offline() {
        chatOnlineService.markOfflineByUserId(currentUserService.requireUserId());
        return Result.success(null);
    }

    private void touchOnline(User user, UserProfile profile, boolean admin) {
        String avatar = profile != null ? profile.getAvatar() : null;
        chatOnlineService.markOnline("http:" + user.getId(), user.getId(), user.getUsername(), avatar, admin);
    }
}
