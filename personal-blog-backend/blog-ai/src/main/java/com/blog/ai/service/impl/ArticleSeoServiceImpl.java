package com.blog.ai.service.impl;

import com.blog.ai.llm.AiService;
import com.blog.ai.mapper.ArticleMapper;
import com.blog.ai.model.entity.Article;
import com.blog.ai.service.ArticleSeoService;
import com.blog.common.dto.SeoGenerateRequest;
import com.blog.common.dto.SeoGenerateResult;
import com.blog.common.exception.ServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleSeoServiceImpl implements ArticleSeoService {

    private final AiService aiService;
    private final ArticleMapper articleMapper;
    private final ObjectMapper objectMapper;

    public ArticleSeoServiceImpl(AiService aiService, ArticleMapper articleMapper, ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.articleMapper = articleMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public SeoGenerateResult generateSeo(Long articleId, SeoGenerateRequest request) {
        String sys = "你是 SEO 编辑。根据下列中文博客标题与正文节选，输出严格一行 JSON，键为 seoTitle、seoDescription。"
                + "seoTitle 简洁不超过30字符；seoDescription 不超过120字符；不要 markdown，不要其它文字。";
        String user = "标题：" + nullToEmpty(request.getTitle())
                + "\n摘要：" + nullToEmpty(request.getSummary())
                + "\n正文节选：\n" + truncate(nullToEmpty(request.getContent()), 6000);
        String raw = aiService.chat(sys, user);
        SeoGenerateResult result = parseSeo(raw);
        applySeo(articleId, result);
        return result;
    }

    @Override
    @Transactional
    public void generateSeoByAi(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new ServiceException(404, "文章不存在");
        }
        SeoGenerateRequest req = new SeoGenerateRequest();
        req.setTitle(article.getTitle());
        req.setSummary(article.getSummary());
        req.setContent(article.getContent());
        generateSeo(articleId, req);
    }

    private void applySeo(Long articleId, SeoGenerateResult result) {
        Article patch = new Article();
        patch.setId(articleId);
        patch.setSeoTitle(result.getSeoTitle());
        patch.setSeoDescription(result.getSeoDescription());
        articleMapper.updateById(patch);
    }

    private SeoGenerateResult parseSeo(String raw) {
        try {
            String s = raw.trim();
            int l = s.indexOf('{');
            int r = s.lastIndexOf('}');
            if (l >= 0 && r > l) {
                s = s.substring(l, r + 1);
            }
            JsonNode node = objectMapper.readTree(s);
            SeoGenerateResult result = new SeoGenerateResult();
            result.setSeoTitle(textOrNull(node.get("seoTitle")));
            result.setSeoDescription(textOrNull(node.get("seoDescription")));
            return result;
        } catch (Exception ex) {
            throw new ServiceException(500, "AI SEO 解析失败");
        }
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String v = node.asText();
        return v.isBlank() ? null : v;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
