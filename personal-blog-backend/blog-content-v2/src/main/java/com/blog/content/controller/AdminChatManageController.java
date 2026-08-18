package com.blog.content.controller;

import com.blog.content.common.support.PageResult;
import com.blog.content.common.support.Result;
import com.blog.content.config.security.CurrentUserService;
import com.blog.content.model.entity.ChatMessage;
import com.blog.content.model.vo.chat.OnlineUserVo;
import com.blog.content.service.ChatManageService;
import com.blog.content.service.ChatOnlineService;
import com.blog.content.service.ChatRecallService;
import com.blog.content.service.UserMuteService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminChatManageController {

    private final ChatManageService chatManageService;
    private final ChatOnlineService chatOnlineService;
    private final ChatRecallService chatRecallService;
    private final UserMuteService userMuteService;
    private final CurrentUserService currentUserService;

    @GetMapping("/chat/messages")
    public Result<PageResult<ChatMessage>> messages(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return Result.success(chatManageService.page(page, size, username, keyword, start, end));
    }

    @DeleteMapping("/chat/messages/{id}")
    public Result<Void> deleteOne(@PathVariable Long id) {
        chatManageService.deleteOne(id);
        return Result.success(null);
    }

    @DeleteMapping("/chat/messages")
    public Result<Void> deleteBatch(@RequestBody Map<String, List<Long>> body) {
        chatManageService.deleteBatch(body.get("ids"));
        return Result.success(null);
    }

    @PostMapping("/chat/messages/{id}/recall")
    public Result<Void> recall(@PathVariable Long id) {
        chatRecallService.recall(id, currentUserService.requireUserId(), true);
        return Result.success(null);
    }

    @GetMapping("/chat/online")
    public Result<List<OnlineUserVo>> online() {
        return Result.success(chatOnlineService.listOnlineSessions());
    }

    @PostMapping("/user/{userId}/mute")
    public Result<Void> mute(@PathVariable Long userId, @RequestParam(defaultValue = "30") int duration) {
        userMuteService.mute(userId, duration);
        return Result.success(null);
    }
}
