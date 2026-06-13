package com.blog.ai.controller;

import com.blog.ai.service.ArticleSeoService;
import com.blog.common.dto.SeoGenerateRequest;
import com.blog.common.dto.SeoGenerateResult;
import com.blog.common.support.Result;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ai/articles")
public class InternalAiController {

    private final ArticleSeoService articleSeoService;

    public InternalAiController(ArticleSeoService articleSeoService) {
        this.articleSeoService = articleSeoService;
    }

    @PostMapping("/{id}/seo")
    public Result<SeoGenerateResult> generateSeo(@PathVariable Long id, @RequestBody SeoGenerateRequest request) {
        return Result.success(articleSeoService.generateSeo(id, request));
    }
}
