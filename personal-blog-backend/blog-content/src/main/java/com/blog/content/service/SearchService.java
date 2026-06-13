package com.blog.content.service;

import com.blog.content.common.support.PageResult;
import com.blog.content.model.vo.search.ArticleSearchHitVo;
import com.blog.content.model.dto.search.SearchPageQuery;

import com.blog.content.model.vo.search.ArticleSearchSuggestVo;

import java.util.List;

public interface SearchService {

    PageResult<ArticleSearchHitVo> searchPublished(SearchPageQuery query);

    List<ArticleSearchSuggestVo> suggest(String keyword, Integer limit);
}
