package com.blog.content.notification.inbox;

import com.blog.content.model.entity.Article;
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
    private final Integer points;
}
