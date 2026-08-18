package com.blog.ai.service;

import com.blog.ai.model.dto.stat.AiUsageItemDto;
import com.blog.ai.model.dto.stat.PvTrendDto;
import com.blog.ai.model.dto.stat.StatsSummaryDto;
import com.blog.ai.model.dto.stat.TopArticleStatDto;

import java.util.List;

public interface AdminStatsService {

    StatsSummaryDto summary();

    PvTrendDto pvTrend(int days);

    List<TopArticleStatDto> topArticles(int limit);

    List<AiUsageItemDto> aiUsage(String period);
}
