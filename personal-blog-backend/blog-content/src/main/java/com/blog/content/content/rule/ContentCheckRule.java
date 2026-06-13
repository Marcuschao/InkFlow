package com.blog.content.content.rule;

import com.blog.content.content.ArticleContentCheckResult;
import com.blog.content.content.ContentCheckContext;

// 设计模式：责任链 + 策略 - 投稿内容审核规则节点，可独立扩展
public interface ContentCheckRule {

    void apply(ContentCheckContext context, ArticleContentCheckResult result);
}
