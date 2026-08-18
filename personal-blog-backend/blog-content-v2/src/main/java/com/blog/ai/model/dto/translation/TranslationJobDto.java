package com.blog.ai.model.dto.translation;

import lombok.Data;

@Data
public class TranslationJobDto {
    private String jobId;
    private String state;
    private int total;
    private int processed;
    private String errorMessage;
}
