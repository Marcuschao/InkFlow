package com.blog.personalblogbackend.model.dto;

import lombok.Data;

@Data
public class ContentReportRequest {
    private String reason;
    private String note;
    private Integer status;
}
