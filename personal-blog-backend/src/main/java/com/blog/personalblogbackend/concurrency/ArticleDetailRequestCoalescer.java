package com.blog.personalblogbackend.concurrency;

import com.blog.personalblogbackend.model.vo.ArticleVO;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class ArticleDetailRequestCoalescer {
    private final ConcurrentHashMap<Long, CompletableFuture<ArticleVO>> inflight = new ConcurrentHashMap<>();

    public ArticleVO coalesce(Long articleId, Supplier<ArticleVO> loader) {
        CompletableFuture<ArticleVO> created = new CompletableFuture<>();
        CompletableFuture<ArticleVO> existing = inflight.putIfAbsent(articleId, created);
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
            inflight.remove(articleId, created);
        }
    }
}
