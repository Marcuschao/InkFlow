package com.blog.content.content;

import com.blog.content.model.entity.Article;
import lombok.Getter;

@Getter
public class ContentCheckContext {

    private final Article article;
    private final Long excludeArticleId;
    private final String title;
    private final String summary;
    private final String content;
    private final String merged;

    public ContentCheckContext(Article article, Long excludeArticleId) {
        this.article = article;
        this.excludeArticleId = excludeArticleId;
        this.title = nullToEmpty(article.getTitle());
        this.summary = nullToEmpty(article.getSummary());
        this.content = nullToEmpty(article.getContent());
        this.merged = title + "\n" + summary + "\n" + content;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
