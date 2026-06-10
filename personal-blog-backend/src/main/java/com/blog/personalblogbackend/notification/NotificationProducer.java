package com.blog.personalblogbackend.notification;

import com.blog.personalblogbackend.config.properties.NotificationRabbitProperties;
import com.blog.personalblogbackend.model.entity.Article;
import com.blog.personalblogbackend.model.enums.NotificationType;
import com.blog.personalblogbackend.notification.inbox.InboxNotificationAssembler;
import com.blog.personalblogbackend.notification.inbox.InboxNotificationDraft;
import com.blog.personalblogbackend.notification.inbox.InboxNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationProducer {

    private static final Logger log = LoggerFactory.getLogger(NotificationProducer.class);

    private final NotificationRabbitProperties props;
    private final RabbitTemplate rabbitTemplate;
    private final InboxNotificationAssembler inboxAssembler;

    public NotificationProducer(NotificationRabbitProperties props,
                                RabbitTemplate rabbitTemplate,
                                InboxNotificationAssembler inboxAssembler) {
        this.props = props;
        this.rabbitTemplate = rabbitTemplate;
        this.inboxAssembler = inboxAssembler;
    }

    public void notifyLike(Long actorUserId, Article article) {
        inboxAssembler.assemble(NotificationType.LIKE, InboxNotificationRequest.builder()
                .actorUserId(actorUserId).article(article).build())
                .ifPresent(this::sendInbox);
    }

    public void notifyFavorite(Long actorUserId, Article article) {
        inboxAssembler.assemble(NotificationType.FAVORITE, InboxNotificationRequest.builder()
                .actorUserId(actorUserId).article(article).build())
                .ifPresent(this::sendInbox);
    }

    public void notifyFollow(Long actorUserId, Long followeeId) {
        inboxAssembler.assemble(NotificationType.FOLLOW, InboxNotificationRequest.builder()
                .actorUserId(actorUserId).followeeId(followeeId).build())
                .ifPresent(this::sendInbox);
    }

    public void notifyComment(Long actorUserId, Long recipientUserId, Long articleId, String articleTitle) {
        inboxAssembler.assemble(NotificationType.COMMENT, InboxNotificationRequest.builder()
                .actorUserId(actorUserId)
                .recipientUserId(recipientUserId)
                .articleId(articleId)
                .articleTitle(articleTitle)
                .build())
                .ifPresent(this::sendInbox);
    }

    public void sendArticlePublished(Article article) {
        if (article == null || article.getId() == null) {
            return;
        }
        NotificationMessage msg = new NotificationMessage();
        msg.setType(DomainEventType.ARTICLE_PUBLISHED.name());
        Map<String, Object> payload = new HashMap<>();
        payload.put("articleId", article.getId());
        payload.put("title", article.getTitle());
        payload.put("summary", article.getSummary());
        msg.setPayload(payload);
        sendAfterCommit(NotificationRabbitProperties.RK_ARTICLE_PUBLISHED, msg);
    }

    public void sendDomainEvent(DomainEvent event) {
        if (event == null || event.getType() == null) {
            return;
        }
        NotificationMessage msg = new NotificationMessage();
        msg.setType(event.getType().name());
        msg.setPayload(event.getPayload());
        Map<String, Object> payload = event.getPayload() != null ? new HashMap<>(event.getPayload()) : new HashMap<>();
        payload.put("eventId", event.getEventId());
        payload.put("occurredAt", event.getOccurredAt() != null ? event.getOccurredAt().toString() : null);
        msg.setPayload(payload);
        if (StringUtils.hasText(event.getEventId())) {
            msg.setEventId(event.getEventId());
        }
        sendAfterCommit(NotificationRabbitProperties.RK_EVENT_PREFIX + event.getType().name(), msg);
    }

    private void sendInbox(InboxNotificationDraft draft) {
        NotificationMessage msg = new NotificationMessage();
        msg.setType(draft.getType().name());
        msg.setRecipientUserId(draft.getRecipientUserId());
        msg.setActorUserId(draft.getActorUserId());
        msg.setTargetId(draft.getTargetId());
        msg.setTargetType(draft.getTargetType().name());
        msg.setContent(draft.getContent());
        sendAfterCommit(routingKeyFor(draft.getType()), msg);
    }

    private String routingKeyFor(NotificationType type) {
        return switch (type) {
            case LIKE -> NotificationRabbitProperties.RK_LIKE;
            case FAVORITE -> NotificationRabbitProperties.RK_FAVORITE;
            case COMMENT -> NotificationRabbitProperties.RK_COMMENT;
            case FOLLOW -> NotificationRabbitProperties.RK_FOLLOW;
        };
    }

    public void sendAfterCommit(String routingKey, NotificationMessage message) {
        if (!props.isEnabled() || message == null) {
            return;
        }
        if (!StringUtils.hasText(message.getEventId())) {
            message.setEventId(UUID.randomUUID().toString());
        }
        Runnable task = () -> {
            try {
                rabbitTemplate.convertAndSend(props.getExchange(), routingKey, message);
                log.debug("[notification] published routingKey={} eventId={}", routingKey, message.getEventId());
            } catch (Exception ex) {
                log.warn("[notification] publish failed routingKey={} eventId={}: {}",
                        routingKey, message.getEventId(), ex.toString());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
