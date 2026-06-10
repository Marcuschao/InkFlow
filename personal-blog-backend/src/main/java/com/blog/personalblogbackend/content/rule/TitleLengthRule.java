package com.blog.personalblogbackend.content.rule;

import com.blog.personalblogbackend.content.ArticleContentCheckResult;
import com.blog.personalblogbackend.content.ContentCheckContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Order(10)
@Component
public class TitleLengthRule implements ContentCheckRule {

    @Override
    public void apply(ContentCheckContext context, ArticleContentCheckResult result) {
        if (!StringUtils.hasText(context.getTitle()) || context.getTitle().length() < 4) {
            result.addIssue("标题过短", 25);
        }
    }
}
