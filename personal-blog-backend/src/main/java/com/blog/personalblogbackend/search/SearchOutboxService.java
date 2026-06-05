package com.blog.personalblogbackend.search;

import com.blog.personalblogbackend.mapper.SearchSyncOutboxMapper;
import com.blog.personalblogbackend.messaging.SearchSyncEventType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@ConditionalOnProperty(name = "blog.search.enabled", havingValue = "true")
public class SearchOutboxService {

    private final SearchSyncOutboxMapper outboxMapper;
    private final SearchOutboxRelay searchOutboxRelay;

    public SearchOutboxService(SearchSyncOutboxMapper outboxMapper, @Lazy SearchOutboxRelay searchOutboxRelay) {
        this.outboxMapper = outboxMapper;
        this.searchOutboxRelay = searchOutboxRelay;
    }

    @Transactional
    public void enqueue(Long articleId, SearchSyncEventType eventType, LocalDateTime articleUpdatedAt) {
        if (articleId == null || eventType == null) {
            return;
        }
        outboxMapper.upsertPending(articleId, eventType.name(), articleUpdatedAt);
        searchOutboxRelay.scheduleFlushAfterCommit();
    }
}
