package com.blog.content.schedule;

import com.blog.content.service.DatabaseBackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseBackupScheduler {

    private final DatabaseBackupService databaseBackupService;

    @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Shanghai")
    public void runDailyBackup() {
        databaseBackupService.runScheduledBackup();
    }
}
