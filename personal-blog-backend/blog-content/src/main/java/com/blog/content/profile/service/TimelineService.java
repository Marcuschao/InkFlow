package com.blog.content.profile.service;

import com.blog.content.profile.model.vo.TimelineItemVo;

import java.util.List;

public interface TimelineService {

    List<TimelineItemVo> interactionTimeline(Long userId, int limit);
}
