package com.blog.content.service;

import com.blog.content.common.support.PageResult;
import com.blog.content.model.vo.notification.NotificationVo;

public interface UserNotificationService {

    PageResult<NotificationVo> pageForUser(Long userId, int page, int size);

    long unreadCount(Long userId);

    void markRead(Long userId, Long notificationId);

    void markAllRead(Long userId);

    void delete(Long userId, Long notificationId);

    void saveFromMessage(com.blog.content.notification.NotificationMessage message);
}
