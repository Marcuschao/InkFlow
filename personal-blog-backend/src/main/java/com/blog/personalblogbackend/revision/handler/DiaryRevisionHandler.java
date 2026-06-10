package com.blog.personalblogbackend.revision.handler;

import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.common.revision.RevisionTargetType;
import com.blog.personalblogbackend.mapper.ContentRevisionMapper;
import com.blog.personalblogbackend.mapper.DiaryMapper;
import com.blog.personalblogbackend.model.entity.ContentRevision;
import com.blog.personalblogbackend.model.entity.Diary;
import com.blog.personalblogbackend.model.vo.revision.ContentRevisionDetailVo;
import com.blog.personalblogbackend.model.vo.revision.ContentRevisionListItemVo;
import com.blog.personalblogbackend.model.vo.revision.RevisionDiffResponseVo;
import com.blog.personalblogbackend.service.RevisionContentStorage;
import com.blog.personalblogbackend.service.RevisionDiffService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DiaryRevisionHandler extends AbstractRevisionTargetHandler {

    private final DiaryMapper diaryMapper;

    public DiaryRevisionHandler(ContentRevisionMapper contentRevisionMapper,
                                RevisionContentStorage revisionContentStorage,
                                RevisionDiffService revisionDiffService,
                                DiaryMapper diaryMapper) {
        super(contentRevisionMapper, revisionContentStorage, revisionDiffService);
        this.diaryMapper = diaryMapper;
    }

    @Override
    public RevisionTargetType type() {
        return RevisionTargetType.DIARY;
    }

    public void snapshot(Diary diary, String remark) {
        if (diary == null || diary.getId() == null) {
            return;
        }
        ContentRevision rev = newRevision(diary.getId());
        rev.setTitle(diary.getTitle());
        String content = nullToEmpty(diary.getContent());
        rev.setContent(content);
        rev.setDiaryDate(diary.getDiaryDate());
        rev.setDiaryTags(diary.getTags());
        rev.setDiaryContentType(diary.getContentType());
        rev.setDiaryIsPublic(diary.getIsPublic());
        rev.setRemark(remark);
        persistSnapshot(rev, content);
    }

    @Override
    public List<ContentRevisionListItemVo> listRevisions(Long targetId, Long userId) {
        requireDiaryOwner(targetId, userId);
        return listRevisionsInternal(targetId);
    }

    @Override
    public ContentRevisionDetailVo getRevision(Long targetId, Long revisionId, Long userId) {
        requireDiaryOwner(targetId, userId);
        return toDetailVo(requireRevision(revisionId, targetId));
    }

    @Override
    @Transactional
    public void restore(Long diaryId, Long revisionId, Long userId) {
        Diary cur = requireDiaryOwner(diaryId, userId);
        ContentRevision target = requireRevision(revisionId, diaryId);
        snapshot(cur, "回退前自动存档");
        applyRevisionToDiary(cur, target);
        cur.setUpdatedAt(LocalDateTime.now());
        diaryMapper.updateById(cur);
        Diary fresh = diaryMapper.selectById(diaryId);
        snapshot(fresh, "从修订 #" + target.getRevisionNo() + " 恢复");
    }

    @Override
    public RevisionDiffResponseVo diff(Long targetId, Long userId, Long leftRevisionId, Long rightRevisionId) {
        requireDiaryOwner(targetId, userId);
        return diffInternal(leftRevisionId, rightRevisionId, targetId);
    }

    private Diary requireDiaryOwner(Long diaryId, Long userId) {
        Diary d = diaryMapper.selectById(diaryId);
        if (d == null || !userId.equals(d.getUserId())) {
            throw new ServiceException(404, "分享不存在");
        }
        return d;
    }

    private void applyRevisionToDiary(Diary cur, ContentRevision target) {
        cur.setTitle(target.getTitle());
        cur.setContent(revisionContentStorage.loadContent(target));
        cur.setDiaryDate(target.getDiaryDate());
        cur.setTags(target.getDiaryTags());
        cur.setContentType(target.getDiaryContentType());
        cur.setIsPublic(target.getDiaryIsPublic());
    }
}
