package com.blog.personalblogbackend.revision.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.common.revision.RevisionTargetType;
import com.blog.personalblogbackend.mapper.ContentRevisionMapper;
import com.blog.personalblogbackend.model.entity.ContentRevision;
import com.blog.personalblogbackend.model.vo.revision.ContentRevisionDetailVo;
import com.blog.personalblogbackend.model.vo.revision.ContentRevisionListItemVo;
import com.blog.personalblogbackend.model.vo.revision.RevisionDiffResponseVo;
import com.blog.personalblogbackend.service.RevisionContentStorage;
import com.blog.personalblogbackend.service.RevisionDiffService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// 设计模式：模板方法 - 固定修订版本号递增、入库与内容持久化流程
public abstract class AbstractRevisionTargetHandler implements RevisionTargetHandler {

    protected final ContentRevisionMapper contentRevisionMapper;
    protected final RevisionContentStorage revisionContentStorage;
    protected final RevisionDiffService revisionDiffService;

    protected AbstractRevisionTargetHandler(ContentRevisionMapper contentRevisionMapper,
                                            RevisionContentStorage revisionContentStorage,
                                            RevisionDiffService revisionDiffService) {
        this.contentRevisionMapper = contentRevisionMapper;
        this.revisionContentStorage = revisionContentStorage;
        this.revisionDiffService = revisionDiffService;
    }

    protected void persistSnapshot(ContentRevision rev, String content) {
        contentRevisionMapper.insert(rev);
        revisionContentStorage.persistAfterInsert(rev, type(), content);
    }

    protected ContentRevision newRevision(Long targetId) {
        ContentRevision rev = new ContentRevision();
        rev.setTargetType(type().name());
        rev.setTargetId(targetId);
        rev.setRevisionNo(nextRevisionNo(targetId));
        rev.setCreatedAt(LocalDateTime.now());
        return rev;
    }

    protected int nextRevisionNo(Long targetId) {
        ContentRevision last = contentRevisionMapper.selectOne(new LambdaQueryWrapper<ContentRevision>()
                .eq(ContentRevision::getTargetType, type().name())
                .eq(ContentRevision::getTargetId, targetId)
                .orderByDesc(ContentRevision::getRevisionNo)
                .last("LIMIT 1"));
        return last == null ? 1 : last.getRevisionNo() + 1;
    }

    protected ContentRevision requireRevision(Long revisionId, Long targetId) {
        ContentRevision r = contentRevisionMapper.selectById(revisionId);
        if (r == null || !type().name().equals(r.getTargetType()) || !targetId.equals(r.getTargetId())) {
            throw new ServiceException(404, "修订不存在");
        }
        return r;
    }

    protected List<ContentRevisionListItemVo> listRevisionsInternal(Long targetId) {
        List<ContentRevision> rows = contentRevisionMapper.selectList(new LambdaQueryWrapper<ContentRevision>()
                .eq(ContentRevision::getTargetType, type().name())
                .eq(ContentRevision::getTargetId, targetId)
                .select(ContentRevision::getId, ContentRevision::getRevisionNo, ContentRevision::getTitle,
                        ContentRevision::getRemark, ContentRevision::getCreatedAt)
                .orderByDesc(ContentRevision::getRevisionNo));
        return rows.stream().map(this::toListItem).collect(Collectors.toList());
    }

    protected ContentRevisionDetailVo toDetailVo(ContentRevision r) {
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

    protected RevisionDiffResponseVo diffInternal(Long leftRevisionId, Long rightRevisionId, Long targetId) {
        ContentRevision l = requireRevision(leftRevisionId, targetId);
        ContentRevision r = requireRevision(rightRevisionId, targetId);
        return revisionDiffService.build(leftRevisionId, rightRevisionId,
                revisionContentStorage.loadContent(l), revisionContentStorage.loadContent(r));
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

    protected static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
