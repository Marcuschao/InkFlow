package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.config.audit.Audit;
import com.blog.personalblogbackend.common.support.PageResult;
import com.blog.personalblogbackend.common.support.Result;
import com.blog.personalblogbackend.model.dto.ArticlePageQuery;
import com.blog.personalblogbackend.model.vo.ArticleSubmitResultVo;
import com.blog.personalblogbackend.model.vo.ArticleVO;
import com.blog.personalblogbackend.model.entity.Article;
import com.blog.personalblogbackend.service.ArticleInteractionEnricher;
import com.blog.personalblogbackend.service.ArticleService;
import com.blog.personalblogbackend.config.security.CurrentUserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;
    @Autowired
    private ArticleInteractionEnricher articleInteractionEnricher;
    @Autowired
    private CurrentUserService currentUserService;
    /**
     * 分页获取文章列表
     * @param query 查询参数
     * @return 统一响应格式的文章分页列表
     */
    @GetMapping
    public Result<PageResult<ArticleVO>> getArticlePage(ArticlePageQuery query) {
        IPage<Article> articlePage = articleService.getArticlePage(query);
        List<ArticleVO> articleVOs = articlePage.getRecords().stream()
                .map(article -> {
                    ArticleVO vo = new ArticleVO();
                    BeanUtils.copyProperties(article, vo);
                    vo.setContent(null);
                    return vo;
                })
                .collect(Collectors.toList());
        return Result.success(PageResult.build(articleVOs, articlePage.getTotal(), articlePage.getSize(), articlePage.getCurrent()));
    }

    @GetMapping("/mine")
    public Result<PageResult<Article>> myArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer status) {
        IPage<Article> p = articleService.getMyArticles(currentUserService.requireUserId(), status, page, size);
        return Result.success(PageResult.build(p.getRecords(), p.getTotal(), p.getSize(), p.getCurrent()));
    }

    /**
     * 获取文章详情
     * @param id 文章ID
     * @return 统一响应格式的文章详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<ArticleVO>> getArticleDetail(WebRequest request,
                                                                @PathVariable Long id,
                                                                @RequestParam(required = false) String lang) {
        Long viewerId = currentUserService.optionalUserId();
        boolean isAdmin = currentUserService.isAdmin();
        ArticleVO vo = articleService.getArticleVo(id, lang, viewerId, isAdmin);
        if (vo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.fail(404, "文章不存在"));
        }
        articleInteractionEnricher.enrich(vo, currentUserService.optionalUserId());
        long lm = 0L;
        if (vo.getUpdateTime() != null) {
            lm = vo.getUpdateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } else if (vo.getCreateTime() != null) {
            lm = vo.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        String etag = "\"" + vo.getId() + "-" + lm + "\"";
        if (request.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        return ResponseEntity.ok().eTag(etag).lastModified(lm).body(Result.success(vo));
    }

    /**
     * 新建文章
     * @param article 文章实体
     * @param tagNames 标签名称列表
     * @return 统一响应格式
     */
    @PostMapping
    public Result<ArticleSubmitResultVo> createArticle(@RequestBody Article article, @RequestParam(required = false) String tagNames) {
        List<String> tags = tagNames != null ? Arrays.asList(tagNames.split(",")) : null;
        ArticleSubmitResultVo vo = articleService.createArticleForUser(
                article, tags, currentUserService.requireUserId(), currentUserService.isAdmin());
        return Result.success(vo);
    }

    /**
     * 更新文章
     * @param id 文章ID
     * @param article 文章实体
     * @param tagNames 标签名称列表
     * @return 统一响应格式
     */
    @PutMapping("/{id}")
    public Result<ArticleSubmitResultVo> updateArticle(@PathVariable Long id, @RequestBody Article article, @RequestParam(required = false) String tagNames) {
        article.setId(id);
        List<String> tags = tagNames != null ? Arrays.asList(tagNames.split(",")) : null;
        ArticleSubmitResultVo vo = articleService.updateArticleForUser(
                article, tags, currentUserService.requireUserId(), currentUserService.isAdmin());
        return Result.success(vo);
    }

    /**
     * 删除文章
     * @param id 文章ID
     * @return 统一响应格式
     */
    @Audit("DELETE_ARTICLE")
    @DeleteMapping("/{id}")
    public Result<String> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticleForUser(id, currentUserService.requireUserId(), currentUserService.isAdmin());
        return Result.success("文章删除成功");
    }
}
