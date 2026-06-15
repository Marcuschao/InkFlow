package com.blog.content.controller;

import com.blog.content.common.support.Result;
import com.blog.content.model.vo.search.ArticleSearchSuggestVo;
import com.blog.content.model.vo.search.SearchResultVo;
import com.blog.content.model.dto.search.SearchPageQuery;
import com.blog.content.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping
    public Result<SearchResultVo> search(SearchPageQuery query) {
        return Result.success(searchService.searchWithRelated(query));
    }

    @GetMapping("/suggest")
    public Result<List<ArticleSearchSuggestVo>> suggest(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer limit) {
        return Result.success(searchService.suggest(keyword, limit));
    }
}
