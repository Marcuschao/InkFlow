package com.blog.personalblogbackend.service;

import com.blog.personalblogbackend.common.support.PageResult;
import com.blog.personalblogbackend.model.vo.search.ArticleSearchHitVo;
import com.blog.personalblogbackend.model.dto.search.SearchPageQuery;

import com.blog.personalblogbackend.model.vo.search.ArticleSearchSuggestVo;

import java.util.List;

public interface SearchService {

    PageResult<ArticleSearchHitVo> searchPublished(SearchPageQuery query);

    List<ArticleSearchSuggestVo> suggest(String keyword, Integer limit);
}
