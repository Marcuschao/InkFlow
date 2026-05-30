package com.blog.personalblogbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.personalblogbackend.config.security.CurrentUserService;
import com.blog.personalblogbackend.model.dto.comment.CommentCreateRequest;
import com.blog.personalblogbackend.model.vo.comment.CommentPublicVo;
import com.blog.personalblogbackend.model.entity.Article;
import com.blog.personalblogbackend.model.entity.Comment;
import com.blog.personalblogbackend.model.entity.User;
import com.blog.personalblogbackend.model.entity.UserProfile;
import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.mapper.CommentMapper;
import com.blog.personalblogbackend.notification.DomainEvent;
import com.blog.personalblogbackend.notification.NotificationProducer;
import com.blog.personalblogbackend.messaging.ContentChangeEventType;
import com.blog.personalblogbackend.messaging.ContentChangeProducer;
import com.blog.personalblogbackend.idempotency.WriteIdempotencyService;
import com.blog.personalblogbackend.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;

    @Autowired
    private com.blog.personalblogbackend.mapper.ArticleMapper articleMapper;
    @Autowired
    private NotificationProducer notificationProducer;
    @Autowired
    private CurrentUserService currentUserService;
    @Autowired
    private com.blog.personalblogbackend.service.UserService userService;
    @Autowired
    private ContentChangeProducer contentChangeProducer;
    @Autowired
    private com.blog.personalblogbackend.service.SensitiveWordService sensitiveWordService;
    @Autowired
    private WriteIdempotencyService writeIdempotencyService;

    @Override
    public List<CommentPublicVo> listApprovedForArticle(Long articleId) {
        List<Comment> list = list(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, articleId)
                .eq(Comment::getStatus, STATUS_APPROVED)
                .orderByAsc(Comment::getCreateTime));
        Set<Long> userIds = list.stream()
                .map(Comment::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserProfile> profileMap = userService.mapProfilesByUserIds(userIds);
        return list.stream()
                .map(c -> toPublicVo(c, profileMap.get(c.getUserId())))
                .collect(Collectors.toList());
    }

    private CommentPublicVo toPublicVo(Comment c, UserProfile profile) {
        String nickname = profile != null && StringUtils.hasText(profile.getNickname())
                ? profile.getNickname()
                : null;
        String avatar = profile != null ? profile.getAvatar() : null;
        return new CommentPublicVo(
                c.getId(),
                c.getParentId(),
                c.getUserId(),
                c.getAuthor(),
                nickname,
                avatar,
                c.getContent(),
                c.getCreateTime());
    }

    @Override
    @Transactional
    public void submit(CommentCreateRequest req) {
        User user = currentUserService.requireUser();
        if (StringUtils.hasText(req.getClientRequestId())) {
            String key = user.getId() + ":comment:" + req.getClientRequestId().trim();
            String cached = writeIdempotencyService.readRawResult("comment", key);
            if (StringUtils.hasText(cached)) {
                return;
            }
        }
        Article a = articleMapper.selectById(req.getArticleId());
        if (a == null || a.getStatus() == null || a.getStatus() != 1) {
            throw new ServiceException(404, "文章不存在或未发布");
        }
        if (req.getParentId() != null) {
            Comment parent = getById(req.getParentId());
            if (parent == null || !parent.getArticleId().equals(req.getArticleId())) {
                throw new ServiceException(400, "回复目标无效");
            }
        }
        if (sensitiveWordService.contains(req.getContent())) {
            throw new ServiceException(400, "内容包含敏感词");
        }
        String author;
        UserProfile profile = userService.mapProfilesByUserIds(List.of(user.getId())).get(user.getId());
        if (profile != null && StringUtils.hasText(profile.getNickname())) {
            author = profile.getNickname();
        } else if (StringUtils.hasText(user.getNickname())) {
            author = user.getNickname();
        } else {
            author = user.getUsername();
        }
        String email = StringUtils.hasText(user.getEmail()) ? user.getEmail().trim().toLowerCase() : "";
        Comment c = new Comment();
        c.setArticleId(req.getArticleId());
        c.setUserId(user.getId());
        c.setParentId(req.getParentId());
        c.setAuthor(author);
        c.setEmail(email);
        c.setContent(req.getContent().trim());
        c.setStatus(STATUS_PENDING);
        c.setCreateTime(LocalDateTime.now());
        save(c);
        if (StringUtils.hasText(req.getClientRequestId())) {
            String key = user.getId() + ":comment:" + req.getClientRequestId().trim();
            writeIdempotencyService.storeResult("comment", key, "OK");
        }
    }

    @Override
    @Transactional
    public void approve(Long id) {
        Comment c = getById(id);
        if (c == null) {
            throw new ServiceException(404, "评论不存在");
        }
        if (c.getStatus() != null && c.getStatus() == STATUS_APPROVED) {
            return;
        }
        c.setStatus(STATUS_APPROVED);
        updateById(c);
        Article article = articleMapper.selectById(c.getArticleId());
        if (article == null) {
            return;
        }
        Long actorId = c.getUserId();
        if (article.getAuthorId() != null) {
            notificationProducer.notifyComment(actorId, article.getAuthorId(), article.getId(), article.getTitle());
        }
        if (c.getParentId() != null) {
            Comment parent = getById(c.getParentId());
            if (parent != null && parent.getUserId() != null) {
                notificationProducer.notifyComment(actorId, parent.getUserId(), article.getId(), article.getTitle());
            }
        }
        notificationProducer.sendDomainEvent(DomainEvent.commentApproved(c));
        contentChangeProducer.send(c.getArticleId(), ContentChangeEventType.COMMENT_APPROVED);
    }

    @Override
    @Transactional
    public void deleteComment(Long id) {
        Comment c = getById(id);
        if (c == null) {
            throw new ServiceException(404, "评论不存在");
        }
        User user = currentUserService.requireUser();
        if (!currentUserService.isAdmin() && (c.getUserId() == null || !c.getUserId().equals(user.getId()))) {
            throw new ServiceException(403, "无权删除该评论");
        }
        removeById(id);
    }

    @Override
    public IPage<Comment> adminPage(int page, int size, Integer status, Long userId) {
        LambdaQueryWrapper<Comment> q = new LambdaQueryWrapper<Comment>().orderByDesc(Comment::getCreateTime);
        if (status != null) {
            q.eq(Comment::getStatus, status);
        }
        if (userId != null) {
            q.eq(Comment::getUserId, userId);
        }
        return page(new Page<>(page, size), q);
    }
}
