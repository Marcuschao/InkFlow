package com.blog.content.cache;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CacheEvictAspect {
    private final ArticleCacheService articleCacheService;
    private final MetaCacheService metaCacheService;

    public CacheEvictAspect(ArticleCacheService articleCacheService, MetaCacheService metaCacheService) {
        this.articleCacheService = articleCacheService;
        this.metaCacheService = metaCacheService;
    }

    @AfterReturning("execution(* com.blog.content.service.impl.ArticleServiceImpl.createArticle(..))")
    public void afterCreate() {
        articleCacheService.evictArticle(null);
    }

    @AfterReturning("execution(* com.blog.content.service.impl.ArticleServiceImpl.updateArticle(..))")
    public void afterUpdate() {
        articleCacheService.evictArticle(null);
    }

    @AfterReturning("execution(* com.blog.content.service.impl.ArticleServiceImpl.deleteArticle(..)) && args(id)")
    public void afterDelete(Long id) {
        articleCacheService.delayDoubleDelete(id);
    }

    @AfterReturning("execution(* com.blog.content.service.impl.TagServiceImpl.save*(..)) "
            + "|| execution(* com.blog.content.service.impl.TagServiceImpl.update*(..)) "
            + "|| execution(* com.blog.content.service.impl.TagServiceImpl.remove*(..))")
    public void afterTagWrite() {
        metaCacheService.evictTags();
    }

    @AfterReturning("execution(* com.blog.content.service.impl.CategoryServiceImpl.save*(..)) "
            + "|| execution(* com.blog.content.service.impl.CategoryServiceImpl.update*(..)) "
            + "|| execution(* com.blog.content.service.impl.CategoryServiceImpl.remove*(..))")
    public void afterCategoryWrite() {
        metaCacheService.evictCategories();
    }
}
