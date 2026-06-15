package com.blog.content.knowledge.service;

import java.util.List;

public interface KnowledgeSubscriptionService {

    void subscribe(Long userId, Long tagId);

    void unsubscribe(Long userId, Long tagId);

    boolean isSubscribed(Long userId, Long tagId);

    void notifySubscribers(Long articleId, List<Long> tagIds, String title);
}
