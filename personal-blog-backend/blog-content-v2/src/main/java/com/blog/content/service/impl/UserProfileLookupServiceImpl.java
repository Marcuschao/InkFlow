package com.blog.content.service.impl;

import com.blog.content.mapper.UserProfileMapper;
import com.blog.content.model.entity.UserProfile;
import com.blog.content.service.UserProfileLookupService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserProfileLookupServiceImpl implements UserProfileLookupService {

    private final UserProfileMapper userProfileMapper;

    public UserProfileLookupServiceImpl(UserProfileMapper userProfileMapper) {
        this.userProfileMapper = userProfileMapper;
    }

    @Override
    public Map<Long, UserProfile> mapProfilesByUserIds(Collection<Long> userIds) {
        Map<Long, UserProfile> map = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return map;
        }
        List<UserProfile> list = userProfileMapper.selectList(new LambdaQueryWrapper<UserProfile>()
                .in(UserProfile::getUserId, userIds));
        for (UserProfile p : list) {
            map.put(p.getUserId(), p);
        }
        return map;
    }
}
