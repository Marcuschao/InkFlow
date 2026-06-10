package com.blog.personalblogbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.personalblogbackend.model.vo.revision.ContentRevisionDetailVo;
import com.blog.personalblogbackend.model.vo.revision.ContentRevisionListItemVo;
import com.blog.personalblogbackend.model.vo.revision.RevisionDiffResponseVo;
import com.blog.personalblogbackend.model.entity.Article;
import com.blog.personalblogbackend.model.entity.ContentRevision;
import com.blog.personalblogbackend.model.entity.Diary;
import com.blog.personalblogbackend.mapper.ContentRevisionMapper;
import com.blog.personalblogbackend.common.revision.RevisionTargetType;
import com.blog.personalblogbackend.revision.handler.ArticleRevisionHandler;
import com.blog.personalblogbackend.revision.handler.DiaryRevisionHandler;
import com.blog.personalblogbackend.service.ContentRevisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContentRevisionServiceImpl implements ContentRevisionService {

    @Autowired
    private ContentRevisionMapper contentRevisionMapper;

    @Autowired
    private ArticleRevisionHandler articleRevisionHandler;

    @Autowired
    private DiaryRevisionHandler diaryRevisionHandler;

    @Override
    public void snapshotArticle(Article article, String articleTagsCsv, String remark) {
        articleRevisionHandler.snapshot(article, articleTagsCsv, remark);
    }

    @Override
    public void snapshotDiary(Diary diary, String remark) {
        diaryRevisionHandler.snapshot(diary, remark);
    }

    @Override
    public void deleteByTarget(RevisionTargetType type, Long targetId) {
        contentRevisionMapper.delete(new LambdaQueryWrapper<ContentRevision>()
                .eq(ContentRevision::getTargetType, type.name())
                .eq(ContentRevision::getTargetId, targetId));
    }

    @Override
    public List<ContentRevisionListItemVo> listArticleRevisions(Long articleId) {
        return articleRevisionHandler.listRevisions(articleId, null);
    }

    @Override
    public ContentRevisionDetailVo getArticleRevision(Long articleId, Long revisionId) {
        return articleRevisionHandler.getRevision(articleId, revisionId, null);
    }

    @Override
    public void restoreArticle(Long articleId, Long revisionId) {
        articleRevisionHandler.restore(articleId, revisionId, null);
    }

    @Override
    public List<ContentRevisionListItemVo> listDiaryRevisions(Long diaryId, Long userId) {
        return diaryRevisionHandler.listRevisions(diaryId, userId);
    }

    @Override
    public ContentRevisionDetailVo getDiaryRevision(Long diaryId, Long revisionId, Long userId) {
        return diaryRevisionHandler.getRevision(diaryId, revisionId, userId);
    }

    @Override
    public void restoreDiary(Long diaryId, Long revisionId, Long userId) {
        diaryRevisionHandler.restore(diaryId, revisionId, userId);
    }

    @Override
    public RevisionDiffResponseVo diffArticleRevisions(Long articleId, Long leftRevisionId, Long rightRevisionId) {
        return articleRevisionHandler.diff(articleId, null, leftRevisionId, rightRevisionId);
    }

    @Override
    public RevisionDiffResponseVo diffDiaryRevisions(Long diaryId, Long userId, Long leftRevisionId, Long rightRevisionId) {
        return diaryRevisionHandler.diff(diaryId, userId, leftRevisionId, rightRevisionId);
    }
}
