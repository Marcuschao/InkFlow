package com.blog.personalblogbackend.content.rule;

import com.blog.personalblogbackend.content.ArticleContentCheckResult;
import com.blog.personalblogbackend.content.ContentCheckContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Order(40)
@Component
public class UrlSpamRule implements ContentCheckRule {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://|www\\.|\\.com|\\.cn|\\.net|加微信|加群|扫码|免费领取|限时优惠",
            Pattern.CASE_INSENSITIVE);

    @Override
    public void apply(ContentCheckContext context, ArticleContentCheckResult result) {
        int urlHits = countMatches(URL_PATTERN, context.getMerged());
        if (urlHits >= 3) {
            result.addIssue("外链/营销链接过多", 25);
        }
    }

    private static int countMatches(Pattern pattern, String text) {
        int n = 0;
        var m = pattern.matcher(text);
        while (m.find()) {
            n++;
        }
        return n;
    }
}
