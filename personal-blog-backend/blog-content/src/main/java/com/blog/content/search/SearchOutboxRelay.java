package com.blog.content.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.content.config.properties.SearchProperties;
import com.blog.content.mapper.SearchSyncOutboxMapper;
import com.blog.content.messaging.SearchIndexProducer;
import com.blog.content.messaging.SearchSyncEventType;
import com.blog.content.model.entity.SearchSyncOutbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(name = "blog.search.enabled", havingValue = "true")
public class SearchOutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(SearchOutboxRelay.class);

    private final SearchSyncOutboxMapper outboxMapper;
    private final SearchIndexProducer searchIndexProducer;
    private final SearchProperties searchProperties;
    private final AtomicBoolean flushScheduled = new AtomicBoolean(false);

    public SearchOutboxRelay(SearchSyncOutboxMapper outboxMapper,
                             SearchIndexProducer searchIndexProducer,
                             SearchProperties searchProperties) {
        this.outboxMapper = outboxMapper;
        this.searchIndexProducer = searchIndexProducer;
        this.searchProperties = searchProperties;
    }

    public void scheduleFlushAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            flushPending();
            return;
        }
        if (!flushScheduled.compareAndSet(false, true)) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                flushScheduled.set(false);
                flushPending();
            }

            @Override
            public void afterCompletion(int status) {
                flushScheduled.set(false);
            }
        });
    }

    @Scheduled(fixedDelayString = "${blog.search.outbox.poll-interval-ms:5000}")
    public void pollOutbox() {
        flushPending();
    }

    public void flushPending() {
        int batchSize = searchProperties.getOutbox().getBatchSize();
        int maxRetries = searchProperties.getOutbox().getMaxRetries();
        List<SearchSyncOutbox> pending = outboxMapper.selectList(
                new LambdaQueryWrapper<SearchSyncOutbox>()
                        .eq(SearchSyncOutbox::getStatus, SearchOutboxStatus.PENDING)
                        .orderByAsc(SearchSyncOutbox::getUpdatedAt)
                        .last("LIMIT " + batchSize));
        for (SearchSyncOutbox row : pending) {
            try {
                SearchSyncEventType type = SearchSyncEventType.valueOf(row.getEventType());
                searchIndexProducer.send(row.getArticleId(), type, row.getArticleUpdatedAt());
                outboxMapper.deleteById(row.getId());
            } catch (Exception ex) {
                int retries = row.getRetryCount() != null ? row.getRetryCount() : 0;
                SearchSyncOutbox patch = new SearchSyncOutbox();
                patch.setId(row.getId());
                patch.setRetryCount(retries + 1);
                patch.setLastError(truncate(ex.toString(), 512));
                outboxMapper.updateById(patch);
                if (retries + 1 >= maxRetries) {
                    log.warn("[search-outbox] publish failed after {} retries articleId={}: {}",
                            maxRetries, row.getArticleId(), ex.toString());
                } else {
                    log.debug("[search-outbox] publish retry {} articleId={}: {}",
                            retries + 1, row.getArticleId(), ex.toString());
                }
            }
        }
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }
}
