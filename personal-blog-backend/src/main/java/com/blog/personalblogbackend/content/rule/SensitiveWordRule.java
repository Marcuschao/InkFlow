package com.blog.personalblogbackend.content.rule;

import com.blog.personalblogbackend.content.ArticleContentCheckResult;
import com.blog.personalblogbackend.content.ContentCheckContext;
import com.blog.personalblogbackend.service.SensitiveWordService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Order(30)
@Component
public class SensitiveWordRule implements ContentCheckRule {

    private final SensitiveWordService sensitiveWordService;

    public SensitiveWordRule(SensitiveWordService sensitiveWordService) {
        this.sensitiveWordService = sensitiveWordService;
    }

    @Override
    public void apply(ContentCheckContext context, ArticleContentCheckResult result) {
        if (sensitiveWordService.contains(context.getMerged())) {
            List<String> hits = sensitiveWordService.findAll(context.getMerged());
            String hint = hits.isEmpty() ? "含敏感词" : "含敏感词：" + String.join(",", hits.stream().limit(3).toList());
            result.addIssue(hint, 40);
        }
    }
}
