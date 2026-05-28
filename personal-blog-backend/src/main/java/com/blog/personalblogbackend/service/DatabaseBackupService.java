package com.blog.personalblogbackend.service;

public interface DatabaseBackupService {

    void runScheduledBackup();

    void triggerBackup();

    BackupStatus getStatus();

    record BackupStatus(String lastTime, Long lastSize, String objectKey, String message) {
    }
}
