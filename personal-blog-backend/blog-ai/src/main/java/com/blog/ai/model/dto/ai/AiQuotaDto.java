package com.blog.ai.model.dto.ai;

import lombok.Data;

@Data
public class AiQuotaDto {
    private long globalDailyTokens;
    private long userDailyTokens;
    private long globalUsed;
}
