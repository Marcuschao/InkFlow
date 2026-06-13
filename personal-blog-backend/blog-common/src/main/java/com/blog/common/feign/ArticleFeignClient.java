package com.blog.common.feign;

import com.blog.common.dto.ArticleInternalDto;
import com.blog.common.dto.ArticleSearchRequest;
import com.blog.common.support.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "blog-content", contextId = "articleFeignClient")
public interface ArticleFeignClient {

    @GetMapping("/internal/articles/{id}")
    Result<ArticleInternalDto> getArticle(@PathVariable("id") Long id);

    @PostMapping("/internal/articles/search")
    Result<List<ArticleInternalDto>> searchArticles(@RequestBody ArticleSearchRequest request);
}
