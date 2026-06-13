package com.blog.content.content.rule;

import com.blog.content.content.ArticleContentCheckResult;
import com.blog.content.content.ContentCheckContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Order(50)
@Component
public class AdKeywordRule implements ContentCheckRule {

    private static final Pattern AD_KEYWORD = Pattern.compile(
            "代理加盟|日赚|刷单|博彩|赌博|贷款|套现|引流|私信领取",
            Pattern.CASE_INSENSITIVE);

    @Override
    public void apply(ContentCheckContext context, ArticleContentCheckResult result) {
        if (AD_KEYWORD.matcher(context.getMerged()).find()) {
            result.addIssue("疑似广告营销内容", 30);
        }
    }
}
