package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.model.vo.revision.ContentRevisionDetailVo;
import com.blog.personalblogbackend.model.vo.revision.ContentRevisionListItemVo;
import com.blog.personalblogbackend.model.vo.revision.RevisionDiffResponseVo;
import com.blog.personalblogbackend.config.security.CurrentUserService;
import com.blog.personalblogbackend.service.ArticleService;
import com.blog.personalblogbackend.service.ContentRevisionService;
import com.blog.personalblogbackend.common.support.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class ArticleRevisionController {

    @Autowired
    private ContentRevisionService contentRevisionService;
    @Autowired
    private ArticleService articleService;
    @Autowired
    private CurrentUserService currentUserService;

    @GetMapping("/{id}/versions")
    public Result<List<ContentRevisionListItemVo>> listVersions(@PathVariable Long id) {
        articleService.requireArticleOwnerOrAdmin(id, currentUserService.requireUserId(), currentUserService.isAdmin());
        return Result.success(contentRevisionService.listArticleRevisions(id));
    }

    @GetMapping("/{id}/versions/{versionId}")
    public Result<ContentRevisionDetailVo> getVersion(@PathVariable Long id, @PathVariable Long versionId) {
        articleService.requireArticleOwnerOrAdmin(id, currentUserService.requireUserId(), currentUserService.isAdmin());
        return Result.success(contentRevisionService.getArticleRevision(id, versionId));
    }

    @PostMapping("/{id}/versions/{versionId}/restore")
    public Result<String> restore(@PathVariable Long id, @PathVariable Long versionId) {
        articleService.requireArticleOwnerOrAdmin(id, currentUserService.requireUserId(), currentUserService.isAdmin());
        contentRevisionService.restoreArticle(id, versionId);
        return Result.success("已回退到指定版本");
    }

    @GetMapping("/{id}/versions/{v1}/diff/{v2}")
    public Result<RevisionDiffResponseVo> diff(@PathVariable Long id, @PathVariable Long v1, @PathVariable Long v2) {
        articleService.requireArticleOwnerOrAdmin(id, currentUserService.requireUserId(), currentUserService.isAdmin());
        return Result.success(contentRevisionService.diffArticleRevisions(id, v1, v2));
    }
}
