package com.blog.content.service;

public interface LogArchiveService {

    void archiveAuditLogs();

    void exportSlowApiSnapshot();
}
