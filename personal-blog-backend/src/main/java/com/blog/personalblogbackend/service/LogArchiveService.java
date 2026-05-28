package com.blog.personalblogbackend.service;

public interface LogArchiveService {

    void archiveAuditLogs();

    void exportSlowApiSnapshot();
}
