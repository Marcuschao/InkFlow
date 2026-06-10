package com.blog.personalblogbackend.notification.inbox;

import com.blog.personalblogbackend.model.enums.NotificationType;

import java.util.Optional;

// 设计模式：策略模式 - 按通知类型构建站内信文案与收件人
public interface InboxNotificationContentStrategy {

    NotificationType type();

    Optional<InboxNotificationDraft> build(InboxNotificationRequest request);
}
