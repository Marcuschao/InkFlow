package com.blog.content.schedule;

import com.blog.content.concurrency.DistributedLockService;
import com.blog.content.knowledge.service.KnowledgeGraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TagCooccurrenceJob {
    private static final Logger log = LoggerFactory.getLogger(TagCooccurrenceJob.class);

    private final KnowledgeGraphService knowledgeGraphService;
    private final DistributedLockService distributedLockService;

    public TagCooccurrenceJob(KnowledgeGraphService knowledgeGraphService,
                              DistributedLockService distributedLockService) {
        this.knowledgeGraphService = knowledgeGraphService;
        this.distributedLockService = distributedLockService;
    }

    @Scheduled(cron = "${blog.knowledge.cooccurrence-cron:0 0 2 * * ?}", zone = "Asia/Shanghai")
    public void run() {
        try {
            boolean executed = distributedLockService.tryExecuteWithLock("lock:knowledge:cooccurrence", 0, () -> {
                knowledgeGraphService.rebuildGraph();
                log.info("[knowledge] cooccurrence job done");
            });
            if (!executed) {
                log.debug("[knowledge] cooccurrence skipped, lock held");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
