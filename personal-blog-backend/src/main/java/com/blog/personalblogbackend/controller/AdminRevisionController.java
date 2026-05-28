package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.common.support.Result;
import com.blog.personalblogbackend.config.audit.Audit;
import com.blog.personalblogbackend.service.RevisionContentStorage;
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
