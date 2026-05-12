package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.audit.Audit;
import com.blog.personalblogbackend.common.Result;
import com.blog.personalblogbackend.service.ArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/articles")
public class AdminArticleSeoController {

    @Autowired
    private ArticleService articleService;

    @Audit("ARTICLE_SEO_AI")
    @PostMapping("/{id}/seo-ai")
    public Result<Void> seoAi(@PathVariable Long id) {
        articleService.generateSeoByAi(id);
        return Result.success(null);
    }
}
