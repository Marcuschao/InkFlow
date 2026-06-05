package com.blog.personalblogbackend.search;

import com.blog.personalblogbackend.config.properties.SearchProperties;
import com.blog.personalblogbackend.mapper.ArticleMapper;
import com.blog.personalblogbackend.messaging.SearchSyncEventType;
import com.blog.personalblogbackend.model.entity.Article;
import com.blog.personalblogbackend.model.enums.ArticleStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnProperty(name = "blog.search.enabled", havingValue = "true")
public class SearchIndexService {

    private final ArticleMapper articleMapper;
    private final MeilisearchClient meilisearchClient;
    private final SearchProperties searchProperties;

    public SearchIndexService(ArticleMapper articleMapper,
                              MeilisearchClient meilisearchClient,
                              SearchProperties searchProperties) {
        this.articleMapper = articleMapper;
        this.meilisearchClient = meilisearchClient;
        this.searchProperties = searchProperties;
    }

    public void sync(Long articleId, SearchSyncEventType eventType, LocalDateTime messageUpdatedAt) {
        if (articleId == null || eventType == null || !meilisearchClient.isAvailable()) {
            return;
        }
        if (eventType == SearchSyncEventType.DELETE) {
            meilisearchClient.deleteDocument(articleId);
            return;
        }
        Article article = articleMapper.selectById(articleId);
        if (article == null || !ArticleStatus.isPublished(article.getStatus())) {
            meilisearchClient.deleteDocument(articleId);
            return;
        }
        if (messageUpdatedAt != null && article.getUpdateTime() != null
                && article.getUpdateTime().isBefore(messageUpdatedAt)) {
            return;
        }
        meilisearchClient.addOrReplaceDocuments(Collections.singletonList(toDocument(article)));
    }

    public void reindexBatch(List<Article> articles) {
        reindexBatch(searchProperties.getIndex(), articles);
    }

    public void reindexBatch(String indexUid, List<Article> articles) {
        if (articles == null || articles.isEmpty() || !meilisearchClient.isAvailable()) {
            return;
        }
        List<ArticleSearchDocument> docs = articles.stream()
                .filter(a -> ArticleStatus.isPublished(a.getStatus()))
                .map(this::toDocument)
                .toList();
        meilisearchClient.addOrReplaceDocuments(indexUid, docs);
    }

    public ArticleSearchDocument toDocument(Article article) {
        ArticleSearchDocument doc = new ArticleSearchDocument();
        doc.setId(article.getId());
        doc.setTitle(article.getTitle());
        doc.setSummary(article.getSummary());
        doc.setContent(truncateContent(article.getContent()));
        doc.setCover(article.getCover());
        doc.setCategoryId(article.getCategoryId());
        doc.setAuthorId(article.getAuthorId());
        doc.setStatus(article.getStatus());
        doc.setViewCount(article.getViewCount());
        doc.setCreateTime(article.getCreateTime());
        doc.setUpdateTime(article.getUpdateTime());
        return doc;
    }

    public boolean isEnabled() {
        return searchProperties.isEnabled();
    }

    private static String truncateContent(String content) {
        if (content == null) {
            return "";
        }
        int max = 12_000;
        return content.length() <= max ? content : content.substring(0, max);
    }
}
