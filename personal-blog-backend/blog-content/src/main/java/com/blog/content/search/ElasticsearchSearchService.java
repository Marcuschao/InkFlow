package com.blog.content.search;

import com.blog.content.common.support.PageResult;
import com.blog.content.model.dto.search.SearchPageQuery;
import com.blog.content.model.vo.search.ArticleSearchHitVo;
import com.blog.content.model.vo.search.ArticleSearchSuggestVo;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "blog.search.enabled", havingValue = "true")
public class ElasticsearchSearchService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final ElasticsearchIndexClient indexClient;

    public ElasticsearchSearchService(ElasticsearchIndexClient indexClient) {
        this.indexClient = indexClient;
    }

    public boolean canSearch() {
        return indexClient.isAvailable();
    }

    public PageResult<ArticleSearchHitVo> search(SearchPageQuery query) {
        String kw = query.getKeyword() != null ? query.getKeyword().trim() : "";
        int page = query.getPage() != null && query.getPage() > 0 ? query.getPage() : 1;
        int size = query.getSize() != null && query.getSize() > 0 ? query.getSize() : 10;
        int offset = (page - 1) * size;
        JsonNode root = indexClient.search(kw, size, offset);
        JsonNode hits = root.path("hits");
        long total = root.path("estimatedTotalHits").asLong(hits.size());
        List<ArticleSearchHitVo> records = new ArrayList<>();
        for (JsonNode hit : hits) {
            records.add(toHitVo(hit));
        }
        return PageResult.build(records, total, size, page);
    }

    public List<ArticleSearchSuggestVo> suggest(String keyword, int limit) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        JsonNode root = indexClient.search(keyword.trim(), limit, 0);
        List<ArticleSearchSuggestVo> list = new ArrayList<>();
        for (JsonNode hit : root.path("hits")) {
            ArticleSearchSuggestVo vo = new ArticleSearchSuggestVo();
            vo.setId(hit.path("id").asLong());
            vo.setTitle(hit.path("title").asText(""));
            vo.setHighlightTitle(HtmlUtils.htmlEscape(vo.getTitle()));
            list.add(vo);
        }
        return list;
    }

    private ArticleSearchHitVo toHitVo(JsonNode hit) {
        ArticleSearchHitVo vo = new ArticleSearchHitVo();
        vo.setId(hit.path("id").asLong());
        vo.setTitle(hit.path("title").asText(""));
        vo.setSummary(hit.path("summary").asText(""));
        vo.setCover(hit.path("cover").asText(null));
        vo.setCreateTime(parseTime(hit.path("createTime").asText(null)));
        vo.setUpdateTime(parseTime(hit.path("updateTime").asText(null)));
        vo.setHighlightTitle(HtmlUtils.htmlEscape(vo.getTitle()));
        vo.setHighlightSummary(HtmlUtils.htmlEscape(StringUtils.hasText(vo.getSummary()) ? vo.getSummary() : ""));
        return vo;
    }

    private static LocalDateTime parseTime(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw, ISO);
        } catch (Exception ex) {
            return null;
        }
    }
}
