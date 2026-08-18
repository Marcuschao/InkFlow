package com.blog.ai.model.dto.ai;

import lombok.Data;

@Data
public class AiStatsOverviewDto {
    private long todayCalls;
    private double successRate;
    private double avgLatencyMs;
    private double totalCost;
}
