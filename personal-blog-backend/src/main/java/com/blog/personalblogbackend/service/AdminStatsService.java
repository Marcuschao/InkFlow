package com.blog.personalblogbackend.service;

import com.blog.personalblogbackend.dto.stat.AiUsageItemDto;
import com.blog.personalblogbackend.dto.stat.PvTrendDto;
import com.blog.personalblogbackend.dto.stat.StatsSummaryDto;
import com.blog.personalblogbackend.dto.stat.TopArticleStatDto;

import java.util.List;

public interface AdminStatsService {

    StatsSummaryDto summary();

    PvTrendDto pvTrend(int days);

    List<TopArticleStatDto> topArticles(int limit);

    List<AiUsageItemDto> aiUsage(String period);
}
