package com.blog.ai.model.dto.ai;

import lombok.Data;

@Data
public class AiModelHealthDto {
    private String providerId;
    private String status;
    private int consecutiveFailures;
    private String lastError;
}
