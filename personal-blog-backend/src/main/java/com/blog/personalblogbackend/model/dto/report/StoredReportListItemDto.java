package com.blog.personalblogbackend.model.dto.report;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StoredReportListItemDto {
    private Long id;
    private String reportType;
    private Long targetId;
    private String title;
    private Long fileSize;
    private LocalDateTime createdAt;
}
