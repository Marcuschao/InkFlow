package com.blog.personalblogbackend.content.rule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.personalblogbackend.content.ArticleContentCheckResult;
import com.blog.personalblogbackend.content.ContentCheckContext;
import com.blog.personalblogbackend.mapper.ArticleMapper;
import com.blog.personalblogbackend.model.entity.Article;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Order(70)
@Component
public class DuplicateContentRule implements ContentCheckRule {

    private final ArticleMapper articleMapper;

    public DuplicateContentRule(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    @Override
    public void apply(ContentCheckContext context, ArticleContentCheckResult result) {
        if (isDuplicate(context.getArticle().getAuthorId(), context.getMerged(), context.getExcludeArticleId())) {
            result.addIssue("与已有投稿内容高度重复", 35);
        }
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
