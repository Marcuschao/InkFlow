package com.blog.content.controller;

import com.blog.content.common.support.Result;
import com.blog.content.model.dto.notification.NotificationMqStatusDto;
import com.blog.content.service.NotificationAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notification-mq")
public class NotificationAdminController {

    private final NotificationAdminService notificationAdminService;

    public NotificationAdminController(NotificationAdminService notificationAdminService) {
        this.notificationAdminService = notificationAdminService;
    }

    @GetMapping("/status")
    public Result<NotificationMqStatusDto> status() {
        return Result.success(notificationAdminService.status());
    }
}
