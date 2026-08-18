package com.blog.ai.model.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiUserUsageDto {
    private Long userId;
    private String username;
    private long count;
}
