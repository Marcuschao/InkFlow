package com.blog.content.profile.service.impl;

import com.blog.content.profile.mapper.ProfileVisitorMapper;
import com.blog.content.profile.model.entity.ProfileVisitor;
import com.blog.content.profile.model.vo.VisitorVo;
import com.blog.content.profile.service.VisitorService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VisitorServiceImpl implements VisitorService {

    private final ProfileVisitorMapper profileVisitorMapper;

    public VisitorServiceImpl(ProfileVisitorMapper profileVisitorMapper) {
        this.profileVisitorMapper = profileVisitorMapper;
    }

    @Override
    @Async
    public void recordVisit(Long ownerId, Long visitorUserId) {
        if (ownerId == null || visitorUserId == null || ownerId.equals(visitorUserId)) {
            return;
        }
        ProfileVisitor row = new ProfileVisitor();
        row.setProfileOwnerId(ownerId);
        row.setVisitorUserId(visitorUserId);
        row.setVisitTime(LocalDateTime.now());
        profileVisitorMapper.insert(row);
    }

    @Override
    public List<VisitorVo> recentVisitors(Long ownerId, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        return profileVisitorMapper.selectRecentVisitors(ownerId, since, Math.min(limit, 50));
    }
}
