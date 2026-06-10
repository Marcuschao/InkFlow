package com.blog.personalblogbackend.notification.inbox;

import com.blog.personalblogbackend.model.entity.Article;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InboxNotificationRequest {

    private final Long actorUserId;
    private final Article article;
    private final Long followeeId;
    private final Long recipientUserId;
    private final Long articleId;
    private final String articleTitle;
}
