package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.audit.Audit;
import com.blog.personalblogbackend.common.PageResult;
import com.blog.personalblogbackend.common.Result;
import com.blog.personalblogbackend.dto.ArticlePageQuery;
import com.blog.personalblogbackend.dto.ArticleVO;
import com.blog.personalblogbackend.entity.Article;
import com.blog.personalblogbackend.service.ArticleService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

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
                    return vo;
                })
                .collect(Collectors.toList());
        return Result.success(PageResult.build(articleVOs, articlePage.getTotal(), articlePage.getSize(), articlePage.getCurrent()));
    }

    /**
     * 获取文章详情
     * @param id 文章ID
     * @return 统一响应格式的文章详情
     */
@GetMapping("/{id}")
    public Result<ArticleVO> getArticleDetail(@PathVariable Long id,
                                            @RequestParam(required = false) String lang) {
        ArticleVO vo = articleService.getArticleVo(id, lang);
        if (vo == null) {
            return Result.fail(404, "文章不存在");
        }
        return Result.success(vo);
    }

    /**
     * 新建文章
     * @param article 文章实体
     * @param tagNames 标签名称列表
     * @return 统一响应格式
     */
    @PostMapping
    public Result<String> createArticle(@RequestBody Article article, @RequestParam(required = false) String tagNames) {
        List<String> tags = tagNames != null ? Arrays.asList(tagNames.split(",")) : null;
        articleService.createArticle(article, tags);
        return Result.success("文章创建成功");
    }

    /**
     * 更新文章
     * @param id 文章ID
     * @param article 文章实体
     * @param tagNames 标签名称列表
     * @return 统一响应格式
     */
    @PutMapping("/{id}")
    public Result<String> updateArticle(@PathVariable Long id, @RequestBody Article article, @RequestParam(required = false) String tagNames) {
        article.setId(id);
        List<String> tags = tagNames != null ? Arrays.asList(tagNames.split(",")) : null;
        articleService.updateArticle(article, tags);
        return Result.success("文章更新成功");
    }

    /**
     * 删除文章
     * @param id 文章ID
     * @return 统一响应格式
     */
    @Audit("DELETE_ARTICLE")
    @DeleteMapping("/{id}")
    public Result<String> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return Result.success("文章删除成功");
    }
}
