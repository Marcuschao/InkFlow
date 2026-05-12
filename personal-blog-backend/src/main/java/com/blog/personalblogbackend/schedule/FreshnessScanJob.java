package com.blog.personalblogbackend.schedule;

import com.blog.personalblogbackend.service.FreshnessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FreshnessScanJob {

    @Autowired
    private FreshnessService freshnessService;

    @Scheduled(cron = "${blog.freshness.scan-cron:0 0 3 * * ?}")
    public void runScheduledFreshnessScan() {
        freshnessService.runFullScan();
    }
}
