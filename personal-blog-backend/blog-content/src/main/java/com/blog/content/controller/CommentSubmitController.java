package com.blog.content.controller;

import com.blog.content.common.support.Result;
import com.blog.content.model.dto.comment.CommentCreateRequest;
import com.blog.content.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
public class CommentSubmitController {

    @Autowired
    private CommentService commentService;

    @PostMapping
    public Result<String> submit(@Valid @RequestBody CommentCreateRequest body) {
        commentService.submit(body);
        return Result.success("提交成功，审核通过后可见");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success(null);
    }
}
