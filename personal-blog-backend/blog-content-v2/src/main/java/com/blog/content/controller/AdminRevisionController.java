package com.blog.content.controller;

import com.blog.content.common.support.Result;
import com.blog.content.config.audit.Audit;
import com.blog.content.service.RevisionContentStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/revisions")
@RequiredArgsConstructor
public class AdminRevisionController {

    private final RevisionContentStorage revisionContentStorage;

    @Audit("REVISION_MIGRATE_MINIO")
    @PostMapping("/migrate")
    public Result<Integer> migrate(@RequestParam(defaultValue = "100") int batchSize) {
        int count = revisionContentStorage.migrateLegacyBatch(batchSize);
        return Result.success(count);
    }
}
