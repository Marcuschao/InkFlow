package com.blog.content.schedule;

import com.blog.content.concurrency.DistributedLockService;
import com.blog.content.knowledge.service.KnowledgeGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TagHotnessJob {
    private static final Logger log = LoggerFactory.getLogger(TagHotnessJob.class);

    private final KnowledgeGraphService knowledgeGraphService;
    private final DistributedLockService distributedLockService;

    public TagHotnessJob(KnowledgeGraphService knowledgeGraphService,
                         DistributedLockService distributedLockService) {
        this.knowledgeGraphService = knowledgeGraphService;
        this.distributedLockService = distributedLockService;
    }

    @Scheduled(cron = "${blog.knowledge.hotness-cron:0 0 * * * ?}", zone = "Asia/Shanghai")
    public void run() {
        try {
            boolean executed = distributedLockService.tryExecuteWithLock("lock:knowledge:hotness", 0, () -> {
                knowledgeGraphService.refreshHotTags();
                log.info("[knowledge] hotness job done");
            });
            if (!executed) {
                log.debug("[knowledge] hotness skipped, lock held");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
