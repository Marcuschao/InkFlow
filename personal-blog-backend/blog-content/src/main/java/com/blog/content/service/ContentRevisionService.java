package com.blog.content.service;

import com.blog.content.model.vo.revision.ContentRevisionDetailVo;
import com.blog.content.model.vo.revision.ContentRevisionListItemVo;
import com.blog.content.model.vo.revision.RevisionDiffResponseVo;
import com.blog.content.model.entity.Article;
import com.blog.content.model.entity.Diary;
import com.blog.content.common.revision.RevisionTargetType;

import java.util.List;

public interface ContentRevisionService {

    void snapshotArticle(Article article, String articleTagsCsv, String remark);

    void snapshotDiary(Diary diary, String remark);

    void deleteByTarget(RevisionTargetType type, Long targetId);

    List<ContentRevisionListItemVo> listArticleRevisions(Long articleId);

    ContentRevisionDetailVo getArticleRevision(Long articleId, Long revisionId);

    void restoreArticle(Long articleId, Long revisionId);

    List<ContentRevisionListItemVo> listDiaryRevisions(Long diaryId, Long userId);

    ContentRevisionDetailVo getDiaryRevision(Long diaryId, Long revisionId, Long userId);

    void restoreDiary(Long diaryId, Long revisionId, Long userId);

    RevisionDiffResponseVo diffArticleRevisions(Long articleId, Long leftRevisionId, Long rightRevisionId);

    RevisionDiffResponseVo diffDiaryRevisions(Long diaryId, Long userId, Long leftRevisionId, Long rightRevisionId);
}
