package com.blog.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.content.model.vo.diary.DiaryPublicDetailVo;
import com.blog.content.model.vo.diary.DiaryPublicListItemVo;
import com.blog.content.model.dto.diary.DiarySaveRequest;
import com.blog.content.model.vo.diary.DiaryVo;
import com.blog.content.model.entity.Diary;
import com.blog.content.common.exception.ServiceException;
import com.blog.content.mapper.DiaryMapper;
import com.blog.content.common.revision.RevisionTargetType;
import com.blog.content.service.ContentRevisionService;
import com.blog.content.service.DiaryService;
import com.blog.content.notification.DomainEvent;
import com.blog.content.notification.NotificationProducer;
import com.blog.content.common.support.PageResult;
import com.blog.content.common.util.DiaryTitleHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DiaryServiceImpl implements DiaryService {

    private final DiaryMapper diaryMapper;

    private final ContentRevisionService contentRevisionService;

    private final NotificationProducer notificationProducer;

    public DiaryServiceImpl(DiaryMapper diaryMapper,
                            ContentRevisionService contentRevisionService,
                            NotificationProducer notificationProducer) {
        this.diaryMapper = diaryMapper;
        this.contentRevisionService = contentRevisionService;
        this.notificationProducer = notificationProducer;
    }

    @Override
    public PageResult<DiaryVo> pageMine(Long userId, int page, int size, String month, String tag) {
        LambdaQueryWrapper<Diary> w = new LambdaQueryWrapper<>();
        w.eq(Diary::getUserId, userId);
        if (StringUtils.hasText(month)) {
            YearMonth ym = YearMonth.parse(month.trim());
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            w.between(Diary::getDiaryDate, start, end);
        }
        if (StringUtils.hasText(tag)) {
            String t = tag.trim();
            w.and(q -> q.like(Diary::getTags, t));
        }
        w.orderByDesc(Diary::getDiaryDate).orderByDesc(Diary::getId);
        Page<Diary> mp = diaryMapper.selectPage(new Page<>(page, size), w);
        List<DiaryVo> records = mp.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.build(records, mp.getTotal(), mp.getSize(), mp.getCurrent());
    }

    @Override
    public DiaryVo getMine(Long userId, Long id) {
        Diary d = diaryMapper.selectById(id);
        if (d == null || !userId.equals(d.getUserId())) {
            throw new ServiceException(404, "分享不存在");
        }
        return toVo(d);
    }

    @Override
    public Long create(Long userId, DiarySaveRequest req) {
        Diary d = new Diary();
        d.setUserId(userId);
        d.setDiaryDate(req.getDiaryDate() != null ? req.getDiaryDate() : LocalDate.now());
        d.setContent(req.getContent());
        d.setContentType(req.getContentType() != null ? req.getContentType() : 0);
        d.setTags(normalizeTags(req.getTags()));
        d.setIsPublic(Boolean.TRUE.equals(req.getIsPublic()) ? 1 : 0);
        d.setTitle(DiaryTitleHelper.resolveTitle(req.getTitle(), req.getContent()));
        diaryMapper.insert(d);
        Diary saved = diaryMapper.selectById(d.getId());
        contentRevisionService.snapshotDiary(saved, "创建");
        notificationProducer.sendDomainEvent(DomainEvent.diaryCreated(saved));
        return d.getId();
    }

    @Override
    public void update(Long userId, Long id, DiarySaveRequest req) {
        Diary existing = diaryMapper.selectById(id);
        if (existing == null || !userId.equals(existing.getUserId())) {
            throw new ServiceException(404, "分享不存在");
        }
        contentRevisionService.snapshotDiary(existing, "保存");
        existing.setDiaryDate(req.getDiaryDate() != null ? req.getDiaryDate() : existing.getDiaryDate());
        existing.setContent(req.getContent());
        existing.setContentType(req.getContentType() != null ? req.getContentType() : 0);
        existing.setTags(normalizeTags(req.getTags()));
        existing.setIsPublic(Boolean.TRUE.equals(req.getIsPublic()) ? 1 : 0);
        existing.setTitle(DiaryTitleHelper.resolveTitle(req.getTitle(), req.getContent()));
        existing.setUpdatedAt(LocalDateTime.now());
        diaryMapper.updateById(existing);
    }

    @Override
    public void delete(Long userId, Long id) {
        Diary existing = diaryMapper.selectById(id);
        if (existing == null || !userId.equals(existing.getUserId())) {
            throw new ServiceException(404, "分享不存在");
        }
        contentRevisionService.deleteByTarget(RevisionTargetType.DIARY, id);
        diaryMapper.deleteById(id);
    }

    @Override
    public PageResult<DiaryPublicListItemVo> pagePublic(int page, int size) {
        LambdaQueryWrapper<Diary> w = new LambdaQueryWrapper<>();
        w.eq(Diary::getIsPublic, 1);
        w.orderByDesc(Diary::getDiaryDate).orderByDesc(Diary::getId);
        Page<Diary> mp = diaryMapper.selectPage(new Page<>(page, size), w);
        List<DiaryPublicListItemVo> records = mp.getRecords().stream().map(this::toPublicListItem).collect(Collectors.toList());
        return PageResult.build(records, mp.getTotal(), mp.getSize(), mp.getCurrent());
    }

    @Override
    public DiaryPublicDetailVo getPublic(Long id) {
        Diary d = diaryMapper.selectById(id);
        if (d == null || d.getIsPublic() == null || d.getIsPublic() != 1) {
            throw new ServiceException(404, "分享不存在或未公开");
        }
        DiaryPublicDetailVo vo = new DiaryPublicDetailVo();
        vo.setId(d.getId());
        vo.setTitle(d.getTitle());
        vo.setDiaryDate(d.getDiaryDate());
        vo.setContent(d.getContent());
        vo.setContentType(d.getContentType());
        vo.setTags(d.getTags());
        vo.setCreatedAt(d.getCreatedAt());
        return vo;
    }

    private DiaryVo toVo(Diary d) {
        DiaryVo vo = new DiaryVo();
        BeanUtils.copyProperties(d, vo);
        return vo;
    }

    private DiaryPublicListItemVo toPublicListItem(Diary d) {
        DiaryPublicListItemVo vo = new DiaryPublicListItemVo();
        vo.setId(d.getId());
        vo.setTitle(d.getTitle());
        vo.setDiaryDate(d.getDiaryDate());
        vo.setTags(d.getTags());
        vo.setCreatedAt(d.getCreatedAt());
        vo.setExcerpt(excerpt(d.getContent()));
        return vo;
    }

    private static String excerpt(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String flat = content.replaceAll("\\R+", " ").trim();
        return DiaryTitleHelper.truncateByCodePoints(flat, 200);
    }

    private static String normalizeTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return "";
        }
        return tags.trim();
    }
}
