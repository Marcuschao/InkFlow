package com.blog.personalblogbackend.service;

import com.blog.personalblogbackend.dto.translation.TranslationJobDto;
import com.blog.personalblogbackend.entity.ArticleTranslation;

import java.util.List;

public interface TranslationService {

    ArticleTranslation getTranslation(Long articleId, String locale);

    void saveTranslation(ArticleTranslation tr);

    void machineTranslate(Long articleId, String locale);

    void generateTranslationSeo(Long articleId, String locale);

    String startBatch(List<Long> articleIds, List<String> locales);

    TranslationJobDto getJob(String jobId);
}
