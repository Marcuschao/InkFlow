package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.dto.revision.ContentRevisionDetailVo;
import com.blog.personalblogbackend.dto.revision.ContentRevisionListItemVo;
import com.blog.personalblogbackend.dto.revision.RevisionDiffResponseVo;
import com.blog.personalblogbackend.service.ContentRevisionService;
import com.blog.personalblogbackend.support.Result;
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

    @GetMapping("/{id}/versions")
    public Result<List<ContentRevisionListItemVo>> listVersions(@PathVariable Long id) {
        return Result.success(contentRevisionService.listArticleRevisions(id));
    }

    @GetMapping("/{id}/versions/{versionId}")
    public Result<ContentRevisionDetailVo> getVersion(@PathVariable Long id, @PathVariable Long versionId) {
        return Result.success(contentRevisionService.getArticleRevision(id, versionId));
    }

    @PostMapping("/{id}/versions/{versionId}/restore")
    public Result<String> restore(@PathVariable Long id, @PathVariable Long versionId) {
        contentRevisionService.restoreArticle(id, versionId);
        return Result.success("已回退到指定版本");
    }

    @GetMapping("/{id}/versions/{v1}/diff/{v2}")
    public Result<RevisionDiffResponseVo> diff(@PathVariable Long id, @PathVariable Long v1, @PathVariable Long v2) {
        return Result.success(contentRevisionService.diffArticleRevisions(id, v1, v2));
    }
}
