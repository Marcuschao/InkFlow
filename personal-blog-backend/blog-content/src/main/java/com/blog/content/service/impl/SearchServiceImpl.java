package com.blog.content.service.impl;

import com.blog.content.common.support.PageResult;
import com.blog.content.config.properties.SearchProperties;
import com.blog.content.model.dto.ArticlePageQuery;
import com.blog.content.model.dto.search.SearchPageQuery;
import com.blog.content.model.entity.Article;
import com.blog.content.model.vo.search.ArticleSearchHitVo;
import com.blog.content.model.vo.search.ArticleSearchSuggestVo;
import com.blog.content.search.ElasticsearchSearchService;
import com.blog.content.service.ArticleService;
import com.blog.content.service.SearchService;
import com.blog.content.common.util.HighlightUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);

    @Autowired
    private ArticleService articleService;

    @Autowired
    private SearchProperties searchProperties;

    @Autowired(required = false)
    private ElasticsearchSearchService meilisearchSearchService;

    @Override
    public PageResult<ArticleSearchHitVo> searchPublished(SearchPageQuery query) {
        if (searchProperties.isEnabled() && meilisearchSearchService != null && meilisearchSearchService.canSearch()) {
            try {
                PageResult<ArticleSearchHitVo> meili = meilisearchSearchService.search(query);
                if (hasHits(meili)) {
                    return meili;
                }
                PageResult<ArticleSearchHitVo> mysql = searchPublishedMysql(query);
                if (hasHits(mysql)) {
                    log.info("[search] Meilisearch empty, fallback MySQL for keyword={}", query.getKeyword());
                    return mysql;
                }
                return meili;
            } catch (Exception ex) {
                log.warn("[search] Meilisearch failed, fallback MySQL: {}", ex.toString());
            }
        }
        return searchPublishedMysql(query);
    }

    @Override
    public List<ArticleSearchSuggestVo> suggest(String keyword, Integer limit) {
        int lim = limit != null && limit > 0 ? Math.min(limit, 20) : 5;
        if (searchProperties.isEnabled() && meilisearchSearchService != null && meilisearchSearchService.canSearch()) {
            try {
                List<ArticleSearchSuggestVo> meili = meilisearchSearchService.suggest(keyword, lim);
                if (!meili.isEmpty()) {
                    return meili;
                }
                List<ArticleSearchSuggestVo> mysql = suggestMysql(keyword, lim);
                if (!mysql.isEmpty()) {
                    return mysql;
                }
                return meili;
            } catch (Exception ex) {
                log.warn("[search] suggest failed, fallback MySQL: {}", ex.toString());
            }
        }
        return suggestMysql(keyword, lim);
    }

    private static boolean hasHits(PageResult<?> page) {
        return page != null && (page.getTotal() > 0 || (page.getRecords() != null && !page.getRecords().isEmpty()));
    }

    private PageResult<ArticleSearchHitVo> searchPublishedMysql(SearchPageQuery query) {
        ArticlePageQuery pq = new ArticlePageQuery();
        pq.setKeyword(StringUtils.hasText(query.getKeyword()) ? query.getKeyword().trim() : null);
        pq.setPage(query.getPage() != null ? query.getPage() : 1);
        pq.setSize(query.getSize() != null ? query.getSize() : 10);

        IPage<Article> page = articleService.getArticlePage(pq);
        String kw = pq.getKeyword() != null ? pq.getKeyword() : "";

        List<ArticleSearchHitVo> hits = page.getRecords().stream().map(a -> {
            ArticleSearchHitVo vo = new ArticleSearchHitVo();
            vo.setId(a.getId());
            vo.setTitle(a.getTitle());
            vo.setSummary(a.getSummary());
            vo.setCover(a.getCover());
            vo.setCreateTime(a.getCreateTime());
            vo.setUpdateTime(a.getUpdateTime());
            vo.setHighlightTitle(HighlightUtils.highlight(a.getTitle(), kw));
            vo.setHighlightSummary(HighlightUtils.highlight(
                    StringUtils.hasText(a.getSummary()) ? a.getSummary() : "", kw));
            return vo;
        }).collect(Collectors.toList());

        return PageResult.build(hits, page.getTotal(), page.getSize(), page.getCurrent());
    }

    private List<ArticleSearchSuggestVo> suggestMysql(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return Collections.emptyList();
        }
        SearchPageQuery q = new SearchPageQuery();
        q.setKeyword(keyword.trim());
        q.setPage(1);
        q.setSize(limit);
        PageResult<ArticleSearchHitVo> page = searchPublishedMysql(q);
        return page.getRecords().stream().map(h -> {
            ArticleSearchSuggestVo vo = new ArticleSearchSuggestVo();
            vo.setId(h.getId());
            vo.setTitle(h.getTitle());
            vo.setHighlightTitle(StringUtils.hasText(h.getHighlightTitle())
                    ? h.getHighlightTitle() : h.getTitle());
            return vo;
        }).collect(Collectors.toList());
    }
}
