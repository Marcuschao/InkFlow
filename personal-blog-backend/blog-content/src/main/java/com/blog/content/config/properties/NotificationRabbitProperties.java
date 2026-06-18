package com.blog.content.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "blog.notification")
public class NotificationRabbitProperties {
    private boolean enabled = true;
    private String exchange = "blog.notification";
    private String inboxQueue = "notification.queue";
    private String pushQueue = "notification.push.queue";
    private String mailQueue = "notification.mail.queue";
    private String auditQueue = "notification.audit.queue";
    private int idempotencyTtlDays = 7;

    public static final String RK_LIKE = "notification.like";
    public static final String RK_FAVORITE = "notification.favorite";
    public static final String RK_COMMENT = "notification.comment";
    public static final String RK_FOLLOW = "notification.follow";
    public static final String RK_REWARD = "notification.reward";
    public static final String RK_ARTICLE_PUBLISHED = "notification.article.published";
    public static final String RK_EVENT_PREFIX = "notification.event.";
}
