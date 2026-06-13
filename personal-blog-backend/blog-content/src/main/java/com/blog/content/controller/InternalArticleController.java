package com.blog.content.controller;

import com.blog.common.dto.ArticleInternalDto;
import com.blog.common.dto.ArticleSearchRequest;
import com.blog.common.support.Result;
import com.blog.content.mapper.ArticleMapper;
import com.blog.content.model.entity.Article;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/internal/articles")
public class InternalArticleController {

    private final ArticleMapper articleMapper;

    public InternalArticleController(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    @GetMapping("/{id}")
    public Result<ArticleInternalDto> get(@PathVariable Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            return Result.fail(404, "文章不存在");
        }
        return Result.success(toDto(article));
    }

    @PostMapping("/search")
    public Result<List<ArticleInternalDto>> search(@RequestBody ArticleSearchRequest request) {
        List<Article> articles;
        if (request.getKeywords() != null && !request.getKeywords().isEmpty()) {
            articles = articleMapper.searchPublishedByKeywords(request.getKeywords(), request.getExcludeId(),
                    request.getLimit() != null ? request.getLimit() : 5);
        } else {
            articles = articleMapper.selectList(new QueryWrapper<Article>()
                    .eq("status", 1).orderByDesc("create_time")
                    .last("LIMIT " + (request.getLimit() != null ? request.getLimit() : 5)));
        }
        List<ArticleInternalDto> list = new ArrayList<>();
        for (Article a : articles) {
            list.add(toDto(a));
        }
        return Result.success(list);
    }

    private ArticleInternalDto toDto(Article article) {
        ArticleInternalDto dto = new ArticleInternalDto();
        BeanUtils.copyProperties(article, dto);
        return dto;
    }
}
