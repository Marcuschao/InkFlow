package com.blog.content.service.impl;

import com.blog.content.gamification.event.ActivityType;
import com.blog.content.gamification.event.UserActivityEventPublisher;
import com.blog.content.mapper.ArticleMapper;
import com.blog.content.model.dto.stat.PageViewRequest;
import com.blog.content.model.entity.Article;
import com.blog.content.model.entity.PageViewEvent;
import com.blog.content.mapper.PageViewEventMapper;
import com.blog.content.service.StatIngestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class StatIngestServiceImpl implements StatIngestService {

    private static final int VIEW_MILESTONE = 1000;

    @Autowired
    private PageViewEventMapper pageViewEventMapper;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private UserActivityEventPublisher activityEventPublisher;

    @Override
    @Async("statExecutor")
    public void recordPageView(PageViewRequest request) {
        if (request == null || !StringUtils.hasText(request.getVisitorId())) {
            return;
        }
        String page = request.getPage() != null ? request.getPage().trim().toLowerCase() : "";
        if (!"home".equals(page) && !"article".equals(page)) {
            return;
        }
        Long articleId = request.getArticleId();
        if ("article".equals(page) && (articleId == null || articleId <= 0)) {
            return;
        }
        if ("home".equals(page)) {
            articleId = null;
        }
        PageViewEvent e = new PageViewEvent();
        e.setPageType(page);
        e.setArticleId(articleId);
        e.setVisitorId(request.getVisitorId().trim());
        e.setCreatedAt(LocalDateTime.now());
        pageViewEventMapper.insert(e);

        if (articleId != null) {
            Article before = articleMapper.selectById(articleId);
            articleMapper.incrementViewCount(articleId);
            if (before != null && before.getAuthorId() != null) {
                int prev = before.getViewCount() != null ? before.getViewCount() : 0;
                int next = prev + 1;
                if (prev < VIEW_MILESTONE && next >= VIEW_MILESTONE) {
                    activityEventPublisher.publish(ActivityType.ARTICLE_VIEWS_UPDATED, before.getAuthorId());
                }
            }
        }
    }
}
