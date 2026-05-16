package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.dto.revision.ContentRevisionDetailVo;
import com.blog.personalblogbackend.dto.revision.ContentRevisionListItemVo;
import com.blog.personalblogbackend.dto.revision.RevisionDiffResponseVo;
import com.blog.personalblogbackend.security.CurrentUserService;
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
@RequestMapping("/api/diary")
public class DiaryRevisionController {

    @Autowired
    private ContentRevisionService contentRevisionService;

    @Autowired
    private CurrentUserService currentUserService;

    @GetMapping("/{id:\\d+}/versions")
    public Result<List<ContentRevisionListItemVo>> listVersions(@PathVariable Long id) {
        Long uid = currentUserService.requireUserId();
        return Result.success(contentRevisionService.listDiaryRevisions(id, uid));
    }

    @GetMapping("/{id:\\d+}/versions/{versionId}")
    public Result<ContentRevisionDetailVo> getVersion(@PathVariable Long id, @PathVariable Long versionId) {
        Long uid = currentUserService.requireUserId();
        return Result.success(contentRevisionService.getDiaryRevision(id, versionId, uid));
    }

    @PostMapping("/{id:\\d+}/versions/{versionId}/restore")
    public Result<String> restore(@PathVariable Long id, @PathVariable Long versionId) {
        Long uid = currentUserService.requireUserId();
        contentRevisionService.restoreDiary(id, versionId, uid);
        return Result.success("已回退到指定版本");
    }

    @GetMapping("/{id:\\d+}/versions/{v1}/diff/{v2}")
    public Result<RevisionDiffResponseVo> diff(@PathVariable Long id, @PathVariable Long v1, @PathVariable Long v2) {
        Long uid = currentUserService.requireUserId();
        return Result.success(contentRevisionService.diffDiaryRevisions(id, uid, v1, v2));
    }
}
