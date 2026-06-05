package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.common.support.PageResult;
import com.blog.personalblogbackend.common.support.Result;
import com.blog.personalblogbackend.model.vo.search.ArticleSearchHitVo;
import com.blog.personalblogbackend.model.vo.search.ArticleSearchSuggestVo;
import com.blog.personalblogbackend.model.dto.search.SearchPageQuery;
import com.blog.personalblogbackend.service.SearchService;
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
    public Result<PageResult<ArticleSearchHitVo>> search(SearchPageQuery query) {
        return Result.success(searchService.searchPublished(query));
    }

    @GetMapping("/suggest")
    public Result<List<ArticleSearchSuggestVo>> suggest(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer limit) {
        return Result.success(searchService.suggest(keyword, limit));
    }
}
