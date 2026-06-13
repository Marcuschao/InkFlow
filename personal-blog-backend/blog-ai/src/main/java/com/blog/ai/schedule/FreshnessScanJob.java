package com.blog.ai.schedule;

import com.blog.ai.concurrency.DistributedLockService;
import com.blog.ai.service.FreshnessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FreshnessScanJob {
    private static final Logger log = LoggerFactory.getLogger(FreshnessScanJob.class);

    @Autowired
    private FreshnessService freshnessService;
    @Autowired
    private DistributedLockService distributedLockService;

    @Scheduled(cron = "${blog.freshness.scan-cron:0 0 3 * * ?}")
    public void runScheduledFreshnessScan() {
        try {
            boolean executed = distributedLockService.tryExecuteWithLock("lock:freshness:scan", 0, () ->
                    freshnessService.runFullScan());
            if (!executed) {
                log.debug("[freshness] scan skipped, lock held");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
