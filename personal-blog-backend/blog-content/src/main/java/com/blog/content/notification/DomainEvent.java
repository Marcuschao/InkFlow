package com.blog.content.notification;

import com.blog.content.model.entity.Article;
import com.blog.content.model.entity.Comment;
import com.blog.content.model.entity.Diary;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent {
    private String eventId;
    private DomainEventType type;
    private Instant occurredAt;
    private Map<String, Object> payload;

    public static DomainEvent of(DomainEventType type, Map<String, Object> payload) {
        DomainEvent e = new DomainEvent();
        e.eventId = UUID.randomUUID().toString();
        e.type = type;
        e.occurredAt = Instant.now();
        e.payload = payload != null ? payload : Map.of();
        return e;
    }

    public static DomainEvent articlePublished(Article a) {
        Map<String, Object> p = new HashMap<>();
        p.put("articleId", a.getId());
        p.put("title", a.getTitle());
        p.put("summary", a.getSummary());
        p.put("status", a.getStatus());
        return of(DomainEventType.ARTICLE_PUBLISHED, p);
    }

    public static DomainEvent articleUpdated(Article a) {
        Map<String, Object> p = new HashMap<>();
        p.put("articleId", a.getId());
        p.put("title", a.getTitle());
        p.put("summary", a.getSummary());
        p.put("status", a.getStatus());
        return of(DomainEventType.ARTICLE_UPDATED, p);
    }

    public static DomainEvent diaryCreated(Diary d) {
        Map<String, Object> p = new HashMap<>();
        p.put("diaryId", d.getId());
        p.put("userId", d.getUserId());
        p.put("title", d.getTitle());
        return of(DomainEventType.DIARY_CREATED, p);
    }

    public static DomainEvent commentApproved(Comment c) {
        Map<String, Object> p = new HashMap<>();
        p.put("commentId", c.getId());
        p.put("articleId", c.getArticleId());
        p.put("userId", c.getUserId());
        p.put("author", c.getAuthor());
        p.put("parentId", c.getParentId());
        return of(DomainEventType.COMMENT_APPROVED, p);
    }
}
