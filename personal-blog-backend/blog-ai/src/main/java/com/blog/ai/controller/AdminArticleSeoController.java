package com.blog.ai.controller;

import com.blog.ai.config.audit.Audit;
import com.blog.ai.common.support.Result;
import com.blog.ai.service.ArticleSeoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/articles")
public class AdminArticleSeoController {

    @Autowired
    private ArticleSeoService articleSeoService;

    @Audit("ARTICLE_SEO_AI")
    @PostMapping("/{id}/seo-ai")
    public Result<Void> seoAi(@PathVariable Long id) {
        articleSeoService.generateSeoByAi(id);
        return Result.success(null);
    }
}
