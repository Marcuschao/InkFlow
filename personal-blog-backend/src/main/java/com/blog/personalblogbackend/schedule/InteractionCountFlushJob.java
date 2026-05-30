package com.blog.personalblogbackend.schedule;

import com.blog.personalblogbackend.concurrency.DistributedLockService;
import com.blog.personalblogbackend.interaction.InteractionRedisStore;
import com.blog.personalblogbackend.mapper.ArticleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class InteractionCountFlushJob {
    private static final Logger log = LoggerFactory.getLogger(InteractionCountFlushJob.class);
    private static final String LOCK_KEY = "lock:interaction:flush";

    private final DistributedLockService distributedLockService;
    private final InteractionRedisStore interactionRedisStore;
    private final ArticleMapper articleMapper;

    public InteractionCountFlushJob(DistributedLockService distributedLockService,
                                    InteractionRedisStore interactionRedisStore,
                                    ArticleMapper articleMapper) {
        this.distributedLockService = distributedLockService;
        this.interactionRedisStore = interactionRedisStore;
        this.articleMapper = articleMapper;
    }

    @Scheduled(fixedDelayString = "${blog.interaction.flush-interval-seconds:30}000")
    public void flushLikeCounts() {
        if (!interactionRedisStore.redisEnabled()) {
            return;
        }
        try {
            distributedLockService.tryExecuteWithLock(LOCK_KEY, 0, () -> {
                Set<String> ids = interactionRedisStore.dirtyLikeArticleIds();
                for (String idStr : ids) {
                    try {
                        Long articleId = Long.valueOf(idStr);
                        Integer count = interactionRedisStore.getLikeCountCached(articleId);
                        if (count != null) {
                            articleMapper.updateLikeCount(articleId, count);
                        }
                        interactionRedisStore.clearDirtyLike(articleId);
                    } catch (Exception ex) {
                        log.warn("flush like count failed articleId={}", idStr, ex);
                    }
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
