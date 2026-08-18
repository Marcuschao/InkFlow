package com.blog.ai.model.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiModelUsageDto {
    private String model;
    private long count;
}
