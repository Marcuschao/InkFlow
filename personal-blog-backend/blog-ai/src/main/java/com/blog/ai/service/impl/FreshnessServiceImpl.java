package com.blog.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.ai.config.properties.BlogFreshnessProperties;
import com.blog.ai.model.dto.freshness.FreshnessAiDraftDto;
import com.blog.ai.model.dto.freshness.FreshnessSummaryDto;
import com.blog.ai.model.entity.Article;
import com.blog.ai.common.exception.ServiceException;
import com.blog.ai.llm.AiService;
import com.blog.ai.mapper.ArticleMapper;
import com.blog.ai.service.FreshnessService;
import com.blog.ai.service.ReportStorageService;
import com.blog.ai.service.SiteKvService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class FreshnessServiceImpl implements FreshnessService {

    public static final String KV_LAST_FULL_SCAN = "freshness_last_full_scan";

    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private SiteKvService siteKvService;
    @Autowired
    private AiService aiService;
    @Autowired
    private BlogFreshnessProperties freshnessProperties;
    @Autowired(required = false)
    private ReportStorageService reportStorageService;

    @Override
    public FreshnessSummaryDto summary() {
        FreshnessSummaryDto dto = new FreshnessSummaryDto();
        dto.setHealthyCount(articleMapper.selectCount(basePublished().eq(Article::getFreshnessStatus, 0)));
        dto.setWarnCount(articleMapper.selectCount(basePublished().eq(Article::getFreshnessStatus, 1)));
        dto.setSevereCount(articleMapper.selectCount(basePublished().eq(Article::getFreshnessStatus, 2)));
        dto.setLastFullScanAt(siteKvService.get(KV_LAST_FULL_SCAN).orElse(null));
        return dto;
    }

    private LambdaQueryWrapper<Article> basePublished() {
        return new LambdaQueryWrapper<Article>().eq(Article::getStatus, 1);
    }

    @Override
    public IPage<Article> pagePublishedByFreshness(Page<Article> page, Integer freshnessStatus) {
        LambdaQueryWrapper<Article> w = basePublished();
        if (freshnessStatus != null) {
            w.eq(Article::getFreshnessStatus, freshnessStatus);
        }
        w.orderByDesc(Article::getUpdateTime);
        return articleMapper.selectPage(page, w);
    }

    @Override
    public FreshnessAiDraftDto aiRefreshDraft(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new ServiceException(404, "文章不存在");
        }
        String sys = "你是资深技术博客编辑。下列 Markdown 正文可能包含过时依赖版本、失效链接描述或陈旧术语。"
                + "请在保留原文结构与核心观点的前提下修订正文：更新明显过时的版本号/年份表述，措辞保持专业简洁。"
                + "直接输出修订后的完整 Markdown 正文，不要前言后记。";
        String body = article.getContent() == null ? "" : article.getContent();
        if (body.length() > 14000) {
            body = body.substring(0, 14000);
        }
        String user = "标题：" + nullToEmpty(article.getTitle())
                + "\n摘要：" + nullToEmpty(article.getSummary())
                + "\n正文：\n" + body;
        String refreshed = aiService.chat(sys, user);
        if (reportStorageService != null && StringUtils.hasText(refreshed)) {
            try {
                reportStorageService.saveFreshnessReport(articleId, article.getTitle(), refreshed);
            } catch (Exception ignored) {
            }
        }
        FreshnessAiDraftDto dto = new FreshnessAiDraftDto();
        dto.setTitle(article.getTitle());
        dto.setSummary(article.getSummary());
        dto.setContent(refreshed);
        return dto;
    }

    @Override
    @Transactional
    public void runFullScan() {
        int warn = freshnessProperties.getWarnDays();
        int severe = freshnessProperties.getSevereDays();
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Article> q = new LambdaQueryWrapper<Article>()
                .select(Article::getId, Article::getUpdateTime, Article::getCreateTime)
                .eq(Article::getStatus, 1);
        articleMapper.selectList(q).forEach(a -> {
            LocalDateTime ref = a.getUpdateTime() != null ? a.getUpdateTime()
                    : (a.getCreateTime() != null ? a.getCreateTime() : now);
            long days = ChronoUnit.DAYS.between(ref.toLocalDate(), now.toLocalDate());
            int st = 0;
            if (days >= severe) {
                st = 2;
            } else if (days >= warn) {
                st = 1;
            }
            Article patch = new Article();
            patch.setId(a.getId());
            patch.setFreshnessStatus(st);
            patch.setFreshnessCheckedAt(now);
            articleMapper.updateById(patch);
        });
        siteKvService.put(KV_LAST_FULL_SCAN, now.toString());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
