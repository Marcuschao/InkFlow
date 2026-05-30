package com.blog.personalblogbackend.service;

import com.blog.personalblogbackend.model.dto.ArticlePageQuery;
import com.blog.personalblogbackend.model.vo.ArticleSubmitResultVo;
import com.blog.personalblogbackend.model.vo.ArticleVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.personalblogbackend.model.entity.Article;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ArticleService extends IService<Article> {

    /**
     * 分页查询文章列表，包含分类名和标签列表
     * @param query 查询参数
     * @return 分页文章列表
     */
    IPage<Article> getArticlePage(ArticlePageQuery query);

    /**
     * 根据ID查询文章详情，包含分类名和标签列表
     * @param id 文章ID
     * @return 文章实体
     */
    Article getArticleDetail(Long id);

    ArticleVO getArticleVo(Long id, String lang, Long viewerUserId, boolean viewerIsAdmin);

    IPage<Article> getMyArticles(Long authorId, Integer status, int page, int size);

    IPage<Article> adminReviewPage(int page, int size);

    ArticleSubmitResultVo createArticleForUser(Article article, List<String> tagNames, Long userId, boolean isAdmin);

    ArticleSubmitResultVo updateArticleForUser(Article article, List<String> tagNames, Long userId, boolean isAdmin);

    void approveArticle(Long articleId, Long reviewerId);

    void rejectArticle(Long articleId, Long reviewerId, String reason);

    void requireArticleOwnerOrAdmin(Long articleId, Long userId, boolean isAdmin);

    Long duplicateArticleAsDraft(Long sourceArticleId);

    void generateSeoByAi(Long articleId);

    /**
     * 创建文章，并处理文章-标签关联
     * @param article 文章实体
     * @param tagNames 标签名称列表
     * @return 是否成功
     */
    boolean createArticle(Article article, List<String> tagNames);

    boolean updateArticle(Article article, List<String> tagNames);

    boolean deleteArticle(Long id);

    boolean deleteArticleForUser(Long id, Long userId, boolean isAdmin);
}
