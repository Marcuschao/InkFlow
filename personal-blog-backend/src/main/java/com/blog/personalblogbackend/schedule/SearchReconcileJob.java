package com.blog.personalblogbackend.schedule;

import com.blog.personalblogbackend.concurrency.DistributedLockService;
import com.blog.personalblogbackend.search.SearchReconcileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "blog.search.enabled", havingValue = "true")
public class SearchReconcileJob {
    private static final Logger log = LoggerFactory.getLogger(SearchReconcileJob.class);

    private final SearchReconcileService searchReconcileService;
    private final DistributedLockService distributedLockService;

    public SearchReconcileJob(SearchReconcileService searchReconcileService,
                              DistributedLockService distributedLockService) {
        this.searchReconcileService = searchReconcileService;
        this.distributedLockService = distributedLockService;
    }

    @Scheduled(cron = "${blog.search.reconcile.cron:0 30 4 * * ?}", zone = "Asia/Shanghai")
    public void runReconcile() {
        try {
            boolean executed = distributedLockService.tryExecuteWithLock("lock:search:reconcile", 0, () -> {
                int n = searchReconcileService.reconcile();
                log.info("[search-reconcile] job done, enqueued={}", n);
            });
            if (!executed) {
                log.debug("[search-reconcile] skipped, lock held");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
