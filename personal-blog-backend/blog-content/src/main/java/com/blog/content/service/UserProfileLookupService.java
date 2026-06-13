package com.blog.content.service;

import com.blog.content.model.entity.UserProfile;

import java.util.Collection;
import java.util.Map;

public interface UserProfileLookupService {
    Map<Long, UserProfile> mapProfilesByUserIds(Collection<Long> userIds);
}
