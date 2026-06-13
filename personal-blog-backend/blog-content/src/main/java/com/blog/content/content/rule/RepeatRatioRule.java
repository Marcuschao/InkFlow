package com.blog.content.content.rule;

import com.blog.content.content.ArticleContentCheckResult;
import com.blog.content.content.ContentCheckContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Order(60)
@Component
public class RepeatRatioRule implements ContentCheckRule {

    @Override
    public void apply(ContentCheckContext context, ArticleContentCheckResult result) {
        if (repeatRatio(context.getTitle(), context.getContent()) > 0.45) {
            result.addIssue("标题与正文重复度过高", 15);
        }
    }

    private static double repeatRatio(String title, String content) {
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            return 0;
        }
        String t = title.trim().toLowerCase(Locale.ROOT);
        String c = content.trim().toLowerCase(Locale.ROOT);
        if (c.contains(t) && t.length() > 8) {
            return (double) t.length() / Math.max(c.length(), 1);
        }
        return 0;
    }
}
