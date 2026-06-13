package com.blog.content.content.rule;

import com.blog.content.content.ArticleContentCheckResult;
import com.blog.content.content.ContentCheckContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Order(20)
@Component
public class ContentLengthRule implements ContentCheckRule {

    @Override
    public void apply(ContentCheckContext context, ArticleContentCheckResult result) {
        if (!StringUtils.hasText(context.getContent()) || context.getContent().trim().length() < 80) {
            result.addIssue("正文过短", 20);
        }
    }
}
