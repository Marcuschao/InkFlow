package com.blog.personalblogbackend.content;

import com.blog.personalblogbackend.content.rule.ContentCheckRule;
import com.blog.personalblogbackend.model.entity.Article;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleContentCheckService {

    private final List<ContentCheckRule> rules;

    public ArticleContentCheckService(List<ContentCheckRule> rules) {
        this.rules = rules;
    }

    // 设计模式：责任链 - 顺序执行各审核规则并汇总结果
    public ArticleContentCheckResult check(Article article, Long excludeArticleId) {
        ArticleContentCheckResult result = new ArticleContentCheckResult();
        ContentCheckContext context = new ContentCheckContext(article, excludeArticleId);
        for (ContentCheckRule rule : rules) {
            rule.apply(context, result);
        }
        return result;
    }
}
