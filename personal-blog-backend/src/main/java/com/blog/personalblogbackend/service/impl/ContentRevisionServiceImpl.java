package com.blog.personalblogbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.personalblogbackend.model.vo.revision.ContentRevisionDetailVo;
import com.blog.personalblogbackend.model.vo.revision.ContentRevisionListItemVo;
import com.blog.personalblogbackend.model.vo.revision.RevisionDiffResponseVo;
import com.blog.personalblogbackend.model.entity.Article;
import com.blog.personalblogbackend.model.entity.ContentRevision;
import com.blog.personalblogbackend.model.entity.Diary;
import com.blog.personalblogbackend.model.entity.Tag;
import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.mapper.ArticleMapper;
import com.blog.personalblogbackend.mapper.ContentRevisionMapper;
import com.blog.personalblogbackend.mapper.DiaryMapper;
import com.blog.personalblogbackend.mapper.TagMapper;
import com.blog.personalblogbackend.common.revision.RevisionTargetType;
import com.blog.personalblogbackend.service.ContentRevisionService;
import com.blog.personalblogbackend.service.RevisionContentStorage;
import com.blog.personalblogbackend.service.RevisionDiffService;
import com.blog.personalblogbackend.notification.DomainEvent;
import com.blog.personalblogbackend.notification.NotificationProducer;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContentRevisionServiceImpl implements ContentRevisionService {

    @Autowired
    private ContentRevisionMapper contentRevisionMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private DiaryMapper diaryMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private RevisionDiffService revisionDiffService;

    @Autowired
    private NotificationProducer notificationProducer;

    @Autowired
    private RevisionContentStorage revisionContentStorage;

    private static boolean isPublished(Integer status) {
        return status != null && status == 1;
    }

    private void publishArticleEvents(Article previous, Article fresh) {
        if (fresh == null || !isPublished(fresh.getStatus())) {
            return;
        }
        if (previous == null || !isPublished(previous.getStatus())) {
            notificationProducer.sendArticlePublished(fresh);
            notificationProducer.sendDomainEvent(DomainEvent.articlePublished(fresh));
        } else {
            notificationProducer.sendDomainEvent(DomainEvent.articleUpdated(fresh));
        }
    }

    @Override
    public void snapshotArticle(Article article, String articleTagsCsv, String remark) {
        if (article == null || article.getId() == null) {
            return;
        }
        ContentRevision rev = new ContentRevision();
        rev.setTargetType(RevisionTargetType.ARTICLE.name());
        rev.setTargetId(article.getId());
        rev.setRevisionNo(nextRevisionNo(RevisionTargetType.ARTICLE, article.getId()));
        rev.setTitle(article.getTitle());
        rev.setSummary(article.getSummary());
        rev.setSeoTitle(article.getSeoTitle());
        rev.setSeoDescription(article.getSeoDescription());
        String content = nullToEmpty(article.getContent());
        rev.setArticleTags(StringUtils.hasText(articleTagsCsv) ? articleTagsCsv.trim() : "");
        rev.setArticleCategoryId(article.getCategoryId());
        rev.setArticleStatus(article.getStatus());
        rev.setArticleCover(article.getCover());
        rev.setRemark(remark);
        rev.setCreatedAt(LocalDateTime.now());
        contentRevisionMapper.insert(rev);
        revisionContentStorage.persistAfterInsert(rev, RevisionTargetType.ARTICLE, content);
    }

    @Override
    public void snapshotDiary(Diary diary, String remark) {
        if (diary == null || diary.getId() == null) {
            return;
        }
        ContentRevision rev = new ContentRevision();
        rev.setTargetType(RevisionTargetType.DIARY.name());
        rev.setTargetId(diary.getId());
        rev.setRevisionNo(nextRevisionNo(RevisionTargetType.DIARY, diary.getId()));
        rev.setTitle(diary.getTitle());
        String content = nullToEmpty(diary.getContent());
        rev.setDiaryDate(diary.getDiaryDate());
        rev.setDiaryTags(diary.getTags());
        rev.setDiaryContentType(diary.getContentType());
        rev.setDiaryIsPublic(diary.getIsPublic());
        rev.setRemark(remark);
        rev.setCreatedAt(LocalDateTime.now());
        contentRevisionMapper.insert(rev);
        revisionContentStorage.persistAfterInsert(rev, RevisionTargetType.DIARY, content);
    }

    @Override
    public void deleteByTarget(RevisionTargetType type, Long targetId) {
        contentRevisionMapper.delete(new LambdaQueryWrapper<ContentRevision>()
                .eq(ContentRevision::getTargetType, type.name())
                .eq(ContentRevision::getTargetId, targetId));
    }

    @Override
    public List<ContentRevisionListItemVo> listArticleRevisions(Long articleId) {
        List<ContentRevision> rows = contentRevisionMapper.selectList(new LambdaQueryWrapper<ContentRevision>()
                .eq(ContentRevision::getTargetType, RevisionTargetType.ARTICLE.name())
                .eq(ContentRevision::getTargetId, articleId)
                .select(ContentRevision::getId, ContentRevision::getRevisionNo, ContentRevision::getTitle,
                        ContentRevision::getRemark, ContentRevision::getCreatedAt)
                .orderByDesc(ContentRevision::getRevisionNo));
        return rows.stream().map(this::toListItem).collect(Collectors.toList());
    }

    @Override
    public ContentRevisionDetailVo getArticleRevision(Long articleId, Long revisionId) {
        ContentRevision r = requireRevision(revisionId, RevisionTargetType.ARTICLE, articleId);
        return toDetailVo(r);
    }

    @Override
    @Transactional
    public void restoreArticle(Long articleId, Long revisionId) {
        Article cur = articleMapper.selectById(articleId);
        if (cur == null) {
            throw new ServiceException(404, "文章不存在");
        }
        ContentRevision target = requireRevision(revisionId, RevisionTargetType.ARTICLE, articleId);
        Article previous = new Article();
        BeanUtils.copyProperties(cur, previous);
        List<String> curTags = articleMapper.selectTagNamesByArticleId(articleId);
        snapshotArticle(cur, String.join(",", curTags), "回退前自动存档");
        applyRevisionToArticle(cur, target);
        articleMapper.updateById(cur);
        syncArticleTagsFromCsv(articleId, target.getArticleTags());
        Article fresh = articleMapper.selectById(articleId);
        List<String> freshTags = articleMapper.selectTagNamesByArticleId(articleId);
        snapshotArticle(fresh, String.join(",", freshTags), "从修订 #" + target.getRevisionNo() + " 恢复");
        publishArticleEvents(previous, fresh);
    }

    @Override
    public List<ContentRevisionListItemVo> listDiaryRevisions(Long diaryId, Long userId) {
        requireDiaryOwner(diaryId, userId);
        List<ContentRevision> rows = contentRevisionMapper.selectList(new LambdaQueryWrapper<ContentRevision>()
                .eq(ContentRevision::getTargetType, RevisionTargetType.DIARY.name())
                .eq(ContentRevision::getTargetId, diaryId)
                .select(ContentRevision::getId, ContentRevision::getRevisionNo, ContentRevision::getTitle,
                        ContentRevision::getRemark, ContentRevision::getCreatedAt)
                .orderByDesc(ContentRevision::getRevisionNo));
        return rows.stream().map(this::toListItem).collect(Collectors.toList());
    }

    @Override
    public ContentRevisionDetailVo getDiaryRevision(Long diaryId, Long revisionId, Long userId) {
        requireDiaryOwner(diaryId, userId);
        ContentRevision r = requireRevision(revisionId, RevisionTargetType.DIARY, diaryId);
        return toDetailVo(r);
    }

    @Override
    @Transactional
    public void restoreDiary(Long diaryId, Long revisionId, Long userId) {
        Diary cur = requireDiaryOwner(diaryId, userId);
        ContentRevision target = requireRevision(revisionId, RevisionTargetType.DIARY, diaryId);
        snapshotDiary(cur, "回退前自动存档");
        applyRevisionToDiary(cur, target);
        cur.setUpdatedAt(LocalDateTime.now());
        diaryMapper.updateById(cur);
        Diary fresh = diaryMapper.selectById(diaryId);
        snapshotDiary(fresh, "从修订 #" + target.getRevisionNo() + " 恢复");
    }

    @Override
    public RevisionDiffResponseVo diffArticleRevisions(Long articleId, Long leftRevisionId, Long rightRevisionId) {
        ContentRevision l = requireRevision(leftRevisionId, RevisionTargetType.ARTICLE, articleId);
        ContentRevision r = requireRevision(rightRevisionId, RevisionTargetType.ARTICLE, articleId);
        return revisionDiffService.build(leftRevisionId, rightRevisionId,
                revisionContentStorage.loadContent(l), revisionContentStorage.loadContent(r));
    }

    @Override
    public RevisionDiffResponseVo diffDiaryRevisions(Long diaryId, Long userId, Long leftRevisionId, Long rightRevisionId) {
        requireDiaryOwner(diaryId, userId);
        ContentRevision l = requireRevision(leftRevisionId, RevisionTargetType.DIARY, diaryId);
        ContentRevision r = requireRevision(rightRevisionId, RevisionTargetType.DIARY, diaryId);
        return revisionDiffService.build(leftRevisionId, rightRevisionId,
                revisionContentStorage.loadContent(l), revisionContentStorage.loadContent(r));
    }

    private void applyRevisionToArticle(Article cur, ContentRevision target) {
        cur.setTitle(target.getTitle());
        cur.setSummary(target.getSummary());
        cur.setSeoTitle(target.getSeoTitle());
        cur.setSeoDescription(target.getSeoDescription());
        cur.setContent(revisionContentStorage.loadContent(target));
        cur.setCategoryId(target.getArticleCategoryId());
        cur.setStatus(target.getArticleStatus());
        cur.setCover(target.getArticleCover());
    }

    private void applyRevisionToDiary(Diary cur, ContentRevision target) {
        cur.setTitle(target.getTitle());
        cur.setContent(revisionContentStorage.loadContent(target));
        cur.setDiaryDate(target.getDiaryDate());
        cur.setTags(target.getDiaryTags());
        cur.setContentType(target.getDiaryContentType());
        cur.setIsPublic(target.getDiaryIsPublic());
    }

    private Diary requireDiaryOwner(Long diaryId, Long userId) {
        Diary d = diaryMapper.selectById(diaryId);
        if (d == null || !userId.equals(d.getUserId())) {
            throw new ServiceException(404, "日记不存在");
        }
        return d;
    }

    private ContentRevision requireRevision(Long revisionId, RevisionTargetType type, Long targetId) {
        ContentRevision r = contentRevisionMapper.selectById(revisionId);
        if (r == null || !type.name().equals(r.getTargetType()) || !targetId.equals(r.getTargetId())) {
            throw new ServiceException(404, "修订不存在");
        }
        return r;
    }

    private int nextRevisionNo(RevisionTargetType type, Long targetId) {
        ContentRevision last = contentRevisionMapper.selectOne(new LambdaQueryWrapper<ContentRevision>()
                .eq(ContentRevision::getTargetType, type.name())
                .eq(ContentRevision::getTargetId, targetId)
                .orderByDesc(ContentRevision::getRevisionNo)
                .last("LIMIT 1"));
        return last == null ? 1 : last.getRevisionNo() + 1;
    }

    private ContentRevisionListItemVo toListItem(ContentRevision r) {
        ContentRevisionListItemVo vo = new ContentRevisionListItemVo();
        vo.setId(r.getId());
        vo.setRevisionNo(r.getRevisionNo());
        vo.setTitle(r.getTitle());
        vo.setRemark(r.getRemark());
        vo.setCreatedAt(r.getCreatedAt());
        return vo;
    }

    private ContentRevisionDetailVo toDetailVo(ContentRevision r) {
        ContentRevisionDetailVo vo = new ContentRevisionDetailVo();
        vo.setId(r.getId());
        vo.setRevisionNo(r.getRevisionNo());
        vo.setTitle(r.getTitle());
        vo.setSummary(r.getSummary());
        vo.setSeoTitle(r.getSeoTitle());
        vo.setSeoDescription(r.getSeoDescription());
        vo.setContent(revisionContentStorage.loadContent(r));
        vo.setArticleTags(r.getArticleTags());
        vo.setArticleCategoryId(r.getArticleCategoryId());
        vo.setArticleStatus(r.getArticleStatus());
        vo.setArticleCover(r.getArticleCover());
        vo.setDiaryDate(r.getDiaryDate());
        vo.setDiaryTags(r.getDiaryTags());
        vo.setDiaryContentType(r.getDiaryContentType());
        vo.setDiaryIsPublic(r.getDiaryIsPublic());
        vo.setRemark(r.getRemark());
        vo.setCreatedAt(r.getCreatedAt());
        return vo;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private void syncArticleTagsFromCsv(Long articleId, String csv) {
        articleMapper.deleteArticleTagsByArticleId(articleId);
        if (!StringUtils.hasText(csv)) {
            return;
        }
        List<String> tagNames = Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(tagNames)) {
            return;
        }
        List<Long> tagIdsToInsert = new ArrayList<>();
        for (String tagName : tagNames) {
            Tag tag = tagMapper.selectOne(new QueryWrapper<Tag>().eq("name", tagName));
            if (tag == null) {
                tag = new Tag();
                tag.setName(tagName);
                tagMapper.insert(tag);
            }
            tagIdsToInsert.add(tag.getId());
        }
        articleMapper.insertArticleTags(articleId, tagIdsToInsert);
    }
}
