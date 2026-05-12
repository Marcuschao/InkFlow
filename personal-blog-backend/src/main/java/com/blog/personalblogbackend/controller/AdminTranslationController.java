package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.audit.Audit;
import com.blog.personalblogbackend.common.Result;
import com.blog.personalblogbackend.dto.translation.TranslationBatchRequest;
import com.blog.personalblogbackend.dto.translation.TranslationJobDto;
import com.blog.personalblogbackend.entity.ArticleTranslation;
import com.blog.personalblogbackend.service.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/translations")
public class AdminTranslationController {

    @Autowired
    private TranslationService translationService;

    @GetMapping("/article/{articleId}")
    public Result<ArticleTranslation> getOne(@PathVariable Long articleId, @RequestParam String locale) {
        return Result.success(translationService.getTranslation(articleId, locale));
    }

    @Audit("TRANSLATION_SAVE")
    @PutMapping("/article/{articleId}")
    public Result<Void> save(@PathVariable Long articleId, @RequestBody ArticleTranslation body) {
        body.setArticleId(articleId);
        translationService.saveTranslation(body);
        return Result.success(null);
    }

    @Audit("TRANSLATION_MACHINE")
    @PostMapping("/article/{articleId}/machine-translate")
    public Result<Void> machine(@PathVariable Long articleId, @RequestParam String locale) {
        translationService.machineTranslate(articleId, locale);
        return Result.success(null);
    }

    @Audit("TRANSLATION_SEO")
    @PostMapping("/article/{articleId}/seo-ai")
    public Result<Void> seoAi(@PathVariable Long articleId, @RequestParam String locale) {
        translationService.generateTranslationSeo(articleId, locale);
        return Result.success(null);
    }

    @Audit("TRANSLATION_BATCH")
    @PostMapping("/batch")
    public Result<String> batch(@RequestBody TranslationBatchRequest request) {
        return Result.success(translationService.startBatch(request.getArticleIds(), request.getLocales()));
    }

    @GetMapping("/jobs/{jobId}")
    public Result<TranslationJobDto> job(@PathVariable String jobId) {
        return Result.success(translationService.getJob(jobId));
    }
}
