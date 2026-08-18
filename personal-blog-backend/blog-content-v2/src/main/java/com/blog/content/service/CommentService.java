package com.blog.content.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.content.model.dto.comment.CommentCreateRequest;
import com.blog.content.model.vo.comment.CommentPublicVo;
import com.blog.content.model.entity.Comment;

import java.util.List;

public interface CommentService extends IService<Comment> {

    List<CommentPublicVo> listApprovedForArticle(Long articleId);

    void submit(CommentCreateRequest req);

    void deleteComment(Long id);

    void approve(Long id);

    com.baomidou.mybatisplus.core.metadata.IPage<Comment> adminPage(int page, int size, Integer status, Long userId);
}
