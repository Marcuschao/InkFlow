package com.blog.content.profile.service.impl;

import com.blog.content.profile.mapper.TimelineMapper;
import com.blog.content.profile.model.vo.TimelineItemVo;
import com.blog.content.profile.service.TimelineService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TimelineServiceImpl implements TimelineService {

    private final TimelineMapper timelineMapper;

    public TimelineServiceImpl(TimelineMapper timelineMapper) {
        this.timelineMapper = timelineMapper;
    }

    @Override
    public List<TimelineItemVo> interactionTimeline(Long userId, int limit) {
        if (userId == null) {
            return List.of();
        }
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return timelineMapper.selectInteractionTimeline(userId, safeLimit);
    }
}
