package com.blog.content.controller;

import com.blog.content.common.support.PageResult;
import com.blog.content.common.support.Result;
import com.blog.content.config.security.CurrentUserService;
import com.blog.content.model.vo.notification.NotificationVo;
import com.blog.content.service.UserNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private UserNotificationService userNotificationService;
    @Autowired
    private CurrentUserService currentUserService;

    @GetMapping
    public Result<PageResult<NotificationVo>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = currentUserService.requireUserId();
        return Result.success(userNotificationService.pageForUser(userId, page, size));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        Long userId = currentUserService.requireUserId();
        return Result.success(Map.of("count", userNotificationService.unreadCount(userId)));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        Long userId = currentUserService.requireUserId();
        userNotificationService.markRead(userId, id);
        return Result.success(null);
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        Long userId = currentUserService.requireUserId();
        userNotificationService.markAllRead(userId);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = currentUserService.requireUserId();
        userNotificationService.delete(userId, id);
        return Result.success(null);
    }
}
