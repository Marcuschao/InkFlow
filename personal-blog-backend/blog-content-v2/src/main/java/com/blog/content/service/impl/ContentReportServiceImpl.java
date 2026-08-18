package com.blog.content.service.impl;

import com.blog.content.common.exception.ServiceException;
import com.blog.content.common.support.PageResult;
import com.blog.content.mapper.ArticleMapper;
import com.blog.content.mapper.ContentReportMapper;
import com.blog.content.model.entity.Article;
import com.blog.content.model.entity.ContentReport;
import com.blog.content.model.enums.ArticleStatus;
import com.blog.content.service.ContentReportService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class ContentReportServiceImpl implements ContentReportService {

    private final ContentReportMapper contentReportMapper;
    private final ArticleMapper articleMapper;

    public ContentReportServiceImpl(ContentReportMapper contentReportMapper, ArticleMapper articleMapper) {
        this.contentReportMapper = contentReportMapper;
        this.articleMapper = articleMapper;
    }

    @Override
    @Transactional
    public void reportArticle(Long articleId, Long reporterId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new ServiceException(400, "请填写举报原因");
        }
        Article article = articleMapper.selectById(articleId);
        if (article == null || !ArticleStatus.isPublished(article.getStatus())) {
            throw new ServiceException(404, "文章不存在");
        }
        ContentReport report = new ContentReport();
        report.setTargetType(ContentReport.TARGET_ARTICLE);
        report.setTargetId(articleId);
        report.setReporterId(reporterId);
        report.setReason(reason.trim());
        report.setStatus(ContentReport.STATUS_PENDING);
        report.setCreateTime(LocalDateTime.now());
        contentReportMapper.insert(report);
    }

    @Override
    public PageResult<ContentReport> adminPage(int page, int size, Integer status) {
        LambdaQueryWrapper<ContentReport> q = new LambdaQueryWrapper<ContentReport>()
                .eq(ContentReport::getTargetType, ContentReport.TARGET_ARTICLE)
                .orderByDesc(ContentReport::getCreateTime);
        if (status != null) {
            q.eq(ContentReport::getStatus, status);
        }
        IPage<ContentReport> p = contentReportMapper.selectPage(
                new Page<>(Math.max(page, 1), Math.max(size, 1)), q);
        return PageResult.build(p.getRecords(), p.getTotal(), p.getSize(), p.getCurrent());
    }

    @Override
    @Transactional
    public void handle(Long reportId, Long handlerId, int status, String note) {
        ContentReport report = contentReportMapper.selectById(reportId);
        if (report == null) {
            throw new ServiceException(404, "举报记录不存在");
        }
        if (status != ContentReport.STATUS_HANDLED && status != ContentReport.STATUS_IGNORED) {
            throw new ServiceException(400, "无效的处理状态");
        }
        report.setStatus(status);
        report.setHandlerId(handlerId);
        report.setHandleNote(StringUtils.hasText(note) ? note.trim() : null);
        report.setHandleTime(LocalDateTime.now());
        contentReportMapper.updateById(report);
    }
}
