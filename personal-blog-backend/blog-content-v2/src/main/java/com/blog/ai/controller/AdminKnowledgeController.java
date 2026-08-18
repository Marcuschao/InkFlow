package com.blog.ai.controller;

import com.blog.ai.common.support.PageResult;
import com.blog.ai.common.support.Result;
import com.blog.ai.model.entity.KnowledgeDocument;
import com.blog.ai.rag.service.KnowledgeDocumentService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/knowledge")
@ConditionalOnProperty(name = "blog.rag.enabled", havingValue = "true")
public class AdminKnowledgeController {

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @PostMapping("/documents")
    public Result<KnowledgeDocument> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(knowledgeDocumentService.upload(file));
    }

    @GetMapping("/documents")
    public Result<PageResult<KnowledgeDocument>> list(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "20") long size,
                                                       @RequestParam(required = false) String status) {
        IPage<KnowledgeDocument> p = knowledgeDocumentService.page(page, size, status);
        return Result.success(PageResult.build(p));
    }

    @GetMapping("/documents/{id}")
    public Result<KnowledgeDocument> get(@PathVariable Long id) {
        return Result.success(knowledgeDocumentService.get(id));
    }

    @DeleteMapping("/documents/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeDocumentService.delete(id);
        return Result.success();
    }
}
