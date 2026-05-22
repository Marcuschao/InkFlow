package com.blog.personalblogbackend.service;

import com.blog.personalblogbackend.common.support.PageResult;
import com.blog.personalblogbackend.model.vo.interaction.FollowStatusVo;
import com.blog.personalblogbackend.model.vo.interaction.FollowToggleVo;
import com.blog.personalblogbackend.model.vo.interaction.UserBriefVo;

public interface UserFollowService {

    FollowToggleVo toggle(Long followerId, Long followeeId);

    FollowStatusVo status(Long followerId, Long followeeId);

    PageResult<UserBriefVo> followers(Long userId, Long viewerId, int page, int size);

    PageResult<UserBriefVo> following(Long userId, Long viewerId, int page, int size);
}
