package com.blog.content.controller;

import com.blog.content.common.support.PageResult;
import com.blog.content.common.support.Result;
import com.blog.content.config.security.CurrentUserService;
import com.blog.content.model.dto.ReviewActionRequest;
import com.blog.content.model.entity.Article;
import com.blog.content.model.vo.ArticleVO;
import com.blog.content.service.ArticleService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/articles")
public class AdminArticleController {

    private final ArticleService articleService;
    private final CurrentUserService currentUserService;

    public AdminArticleController(ArticleService articleService, CurrentUserService currentUserService) {
        this.articleService = articleService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/review")
    public Result<PageResult<ArticleVO>> reviewPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        IPage<Article> p = articleService.adminReviewPage(page, size);
        List<ArticleVO> vos = p.getRecords().stream().map(a -> {
            ArticleVO vo = new ArticleVO();
            BeanUtils.copyProperties(a, vo);
            return vo;
        }).collect(Collectors.toList());
        return Result.success(PageResult.build(vos, p.getTotal(), p.getSize(), p.getCurrent()));
    }

    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        articleService.approveArticle(id, currentUserService.requireUserId());
        return Result.success(null);
    }

    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) ReviewActionRequest body) {
        String reason = body != null ? body.getReason() : null;
        articleService.rejectArticle(id, currentUserService.requireUserId(), reason);
        return Result.success(null);
    }
}
