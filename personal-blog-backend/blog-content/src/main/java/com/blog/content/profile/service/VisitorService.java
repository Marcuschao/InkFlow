package com.blog.content.profile.service;

import com.blog.content.profile.model.vo.VisitorVo;

import java.util.List;

public interface VisitorService {

    void recordVisit(Long ownerId, Long visitorUserId);

    List<VisitorVo> recentVisitors(Long ownerId, int limit);
}
