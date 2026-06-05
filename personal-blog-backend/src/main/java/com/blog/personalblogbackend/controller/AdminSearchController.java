package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.common.support.Result;
import com.blog.personalblogbackend.search.SearchReindexResult;
import com.blog.personalblogbackend.search.SearchReindexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/search")
@ConditionalOnBean(SearchReindexService.class)
public class AdminSearchController {

    @Autowired
    private SearchReindexService searchReindexService;

    @PostMapping("/reindex")
    public Result<Map<String, Object>> reindex() {
        SearchReindexResult result = searchReindexService.reindexAllPublished();
        return Result.success(Map.of(
                "indexed", result.getIndexed(),
                "rebuildIndex", result.getRebuildIndex(),
                "swapped", result.isSwapped()));
    }
}
