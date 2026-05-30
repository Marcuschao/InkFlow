package com.blog.personalblogbackend.content;

import com.blog.personalblogbackend.mapper.ArticleMapper;
import com.blog.personalblogbackend.model.entity.Article;
import com.blog.personalblogbackend.service.SensitiveWordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ArticleContentCheckService {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://|www\\.|\\.com|\\.cn|\\.net|加微信|加群|扫码|免费领取|限时优惠",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern AD_KEYWORD = Pattern.compile(
            "代理加盟|日赚|刷单|博彩|赌博|贷款|套现|引流|私信领取",
            Pattern.CASE_INSENSITIVE);

    private final SensitiveWordService sensitiveWordService;
    private final ArticleMapper articleMapper;

    public ArticleContentCheckService(SensitiveWordService sensitiveWordService, ArticleMapper articleMapper) {
        this.sensitiveWordService = sensitiveWordService;
        this.articleMapper = articleMapper;
    }

    public ArticleContentCheckResult check(Article article, Long excludeArticleId) {
        ArticleContentCheckResult result = new ArticleContentCheckResult();
        String title = nullToEmpty(article.getTitle());
        String summary = nullToEmpty(article.getSummary());
        String content = nullToEmpty(article.getContent());
        String merged = title + "\n" + summary + "\n" + content;

        if (!StringUtils.hasText(title) || title.length() < 4) {
            result.addIssue("标题过短", 25);
        }
        if (!StringUtils.hasText(content) || content.trim().length() < 80) {
            result.addIssue("正文过短", 20);
        }

        if (sensitiveWordService.contains(merged)) {
            List<String> hits = sensitiveWordService.findAll(merged);
            String hint = hits.isEmpty() ? "含敏感词" : "含敏感词：" + String.join(",", hits.stream().limit(3).toList());
            result.addIssue(hint, 40);
        }

        int urlHits = countMatches(URL_PATTERN, merged);
        if (urlHits >= 3) {
            result.addIssue("外链/营销链接过多", 25);
        }
        if (AD_KEYWORD.matcher(merged).find()) {
            result.addIssue("疑似广告营销内容", 30);
        }

        if (repeatRatio(title, content) > 0.45) {
            result.addIssue("标题与正文重复度过高", 15);
        }

        if (isDuplicate(article.getAuthorId(), merged, excludeArticleId)) {
            result.addIssue("与已有投稿内容高度重复", 35);
        }

        return result;
    }

    private static int countMatches(Pattern pattern, String text) {
        int n = 0;
        var m = pattern.matcher(text);
        while (m.find()) {
            n++;
        }
        return n;
    }

    private static double repeatRatio(String title, String content) {
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            return 0;
        }
        String t = title.trim().toLowerCase(Locale.ROOT);
        String c = content.trim().toLowerCase(Locale.ROOT);
        if (c.contains(t) && t.length() > 8) {
            return (double) t.length() / Math.max(c.length(), 1);
        }
        return 0;
    }

    private boolean isDuplicate(Long authorId, String merged, Long excludeArticleId) {
        if (authorId == null) {
            return false;
        }
        String hash = sha256(normalize(merged));
        LambdaQueryWrapper<Article> q = new LambdaQueryWrapper<Article>()
                .eq(Article::getAuthorId, authorId)
                .in(Article::getStatus, 1, 2, 3)
                .orderByDesc(Article::getUpdateTime)
                .last("LIMIT 20");
        if (excludeArticleId != null) {
            q.ne(Article::getId, excludeArticleId);
        }
        List<Article> recent = articleMapper.selectList(q);
        for (Article a : recent) {
            String other = normalize(nullToEmpty(a.getTitle()) + "\n" + nullToEmpty(a.getSummary()) + "\n"
                    + nullToEmpty(a.getContent()));
            if (sha256(other).equals(hash)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String s) {
        return s.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            return s;
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
