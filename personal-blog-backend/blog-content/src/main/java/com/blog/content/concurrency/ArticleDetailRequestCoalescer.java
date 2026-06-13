package com.blog.content.concurrency;

import com.blog.content.model.vo.ArticleVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class ArticleDetailRequestCoalescer {
    private final ConcurrentHashMap<String, CompletableFuture<ArticleVO>> inflight = new ConcurrentHashMap<>();

    public ArticleVO coalesce(Long articleId, String lang, Supplier<ArticleVO> loader) {
        String key = coalesceKey(articleId, lang);
        CompletableFuture<ArticleVO> created = new CompletableFuture<>();
        CompletableFuture<ArticleVO> existing = inflight.putIfAbsent(key, created);
        if (existing != null) {
            return existing.join();
        }
        try {
            ArticleVO result = loader.get();
            created.complete(result);
            return result;
        } catch (Throwable ex) {
            created.completeExceptionally(ex);
            throw ex;
        } finally {
            inflight.remove(key, created);
        }
    }

    static String coalesceKey(Long articleId, String lang) {
        String loc = "zh";
        if (StringUtils.hasText(lang)) {
            loc = lang.trim().toLowerCase();
        }
        return articleId + ":" + loc;
    }
}
