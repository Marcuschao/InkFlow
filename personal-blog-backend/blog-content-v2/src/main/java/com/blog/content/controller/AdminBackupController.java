package com.blog.content.controller;

import com.blog.content.common.support.Result;
import com.blog.content.config.audit.Audit;
import com.blog.content.service.DatabaseBackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/backup")
@RequiredArgsConstructor
public class AdminBackupController {

    private final DatabaseBackupService databaseBackupService;

    @GetMapping("/status")
    public Result<DatabaseBackupService.BackupStatus> status() {
        return Result.success(databaseBackupService.getStatus());
    }

    @Audit("DB_BACKUP_TRIGGER")
    @PostMapping("/trigger")
    public Result<DatabaseBackupService.BackupStatus> trigger() {
        databaseBackupService.triggerBackup();
        return Result.success(databaseBackupService.getStatus());
    }
}
