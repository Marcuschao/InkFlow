package com.blog.personalblogbackend.service;

import com.blog.personalblogbackend.dto.agent.*;

import java.util.List;

public interface AgentService {

    String outline(OutlineRequest request);

    String expand(ExpandRequest request);

    String polish(PolishRequest request);

    String editorOutline(EditorOutlineRequest request);

    String editorContinue(EditorContinueRequest request);

    String editorPolish(EditorPolishRequest request);

    String summary(SummaryRequest request);

    List<String> tags(TagsRequest request);

    ChatResponse chat(ChatRequest request);

    List<RecommendArticleDto> recommend(Long articleId);

    List<RecommendArticleDto> recommendWithContext(Long articleId, List<Long> recentArticleIds);

    List<RecommendArticleDto> recommendHome(List<Long> recentArticleIds);

    String weeklyReport(WeeklyReportRequest request);
}
