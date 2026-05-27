package com.blog.personalblogbackend.cache;

import com.blog.personalblogbackend.mapper.ArticleMapper;
import com.blog.personalblogbackend.model.entity.Article;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class ArticleBloomFilter {
    private static final int EXPECTED = 100_000;
    private static final double FPP = 0.01;

    private final ArticleMapper articleMapper;
    private volatile BloomFilter<String> filter;

    public ArticleBloomFilter(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    @PostConstruct
    public void init() {
        reload();
    }

    public void reload() {
        BloomFilter<String> next = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8), EXPECTED, FPP);
        List<Article> articles = articleMapper.selectList(null);
        for (Article article : articles) {
            if (article.getId() != null) {
                next.put(String.valueOf(article.getId()));
            }
        }
        filter = next;
    }

    public void add(Long articleId) {
        if (articleId != null && filter != null) {
            filter.put(String.valueOf(articleId));
        }
    }

    public boolean mightContain(Long articleId) {
        if (articleId == null || filter == null) {
            return true;
        }
        return filter.mightContain(String.valueOf(articleId));
    }
}
