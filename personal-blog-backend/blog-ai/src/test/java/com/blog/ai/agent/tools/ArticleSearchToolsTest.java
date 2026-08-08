package com.blog.ai.agent.tools;

import com.blog.ai.mapper.ArticleMapper;
import com.blog.ai.model.entity.Article;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArticleSearchToolsTest {
    @Test
    void concurrentCallsReturnIndependentStructuredResults() {
        ArticleMapper mapper = mock(ArticleMapper.class);
        when(mapper.searchPublishedByKeywords(any(), isNull(), anyInt())).thenAnswer(invocation -> {
            String keyword = ((List<String>) invocation.getArgument(0)).get(0);
            Article article = new Article(); article.setId("alpha".equals(keyword) ? 1L : 2L);
            article.setTitle(keyword); article.setContent("content-" + keyword); return List.of(article);
        });
        ArticleSearchTools tools = new ArticleSearchTools(mapper);
        var first = CompletableFuture.supplyAsync(() -> tools.searchStructured("alpha"));
        var second = CompletableFuture.supplyAsync(() -> tools.searchStructured("beta"));
        assertEquals("alpha", first.join().get(0).getTitle());
        assertEquals("beta", second.join().get(0).getTitle());
    }
}
