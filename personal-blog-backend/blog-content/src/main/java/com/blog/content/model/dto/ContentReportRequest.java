package com.blog.content.model.dto;

import lombok.Data;

@Data
public class ContentReportRequest {
    private String reason;
    private String note;
    private Integer status;
}
