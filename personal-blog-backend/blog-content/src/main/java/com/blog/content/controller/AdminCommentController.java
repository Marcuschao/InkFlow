package com.blog.content.controller;

import com.blog.content.common.support.PageResult;
import com.blog.content.common.support.Result;
import com.blog.content.model.entity.Comment;
import com.blog.content.service.CommentService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    @Autowired
    private CommentService commentService;

    @GetMapping
    public Result<PageResult<Comment>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long userId) {
        IPage<Comment> p = commentService.adminPage(page, size, status, userId);
        return Result.success(PageResult.build(p.getRecords(), p.getTotal(), p.getSize(), p.getCurrent()));
    }

    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        commentService.approve(id);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success(null);
    }
}
