package com.blog.content.schedule;

import com.blog.content.service.LogArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogArchiveScheduler {

    private final LogArchiveService logArchiveService;

    @Scheduled(cron = "0 0 2 ? * SUN", zone = "Asia/Shanghai")
    public void runWeeklyArchive() {
        logArchiveService.archiveAuditLogs();
        logArchiveService.exportSlowApiSnapshot();
    }
}
