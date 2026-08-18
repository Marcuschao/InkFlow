package com.blog.content.service.impl;

import com.blog.content.common.exception.ServiceException;
import com.blog.content.mapper.UserMapper;
import com.blog.content.mapper.UserProfileMapper;
import com.blog.content.model.entity.User;
import com.blog.content.model.entity.UserProfile;
import com.blog.content.model.vo.user.PublicUserVo;
import com.blog.content.service.UserPublicService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserPublicServiceImpl implements UserPublicService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;

    public UserPublicServiceImpl(UserMapper userMapper,
                                 UserProfileMapper userProfileMapper) {
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
    }

    @Override
    public PublicUserVo getPublicProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(404, "用户不存在");
        }
        UserProfile profile = userProfileMapper.selectById(userId);
        String nickname = profile != null && StringUtils.hasText(profile.getNickname())
                ? profile.getNickname()
                : (StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        String region = profile != null && StringUtils.hasText(profile.getRegion())
                ? profile.getRegion()
                : (profile != null ? profile.getLastLoginRegion() : user.getRegisterRegion());
        return new PublicUserVo(
                user.getId(),
                nickname,
                profile != null ? profile.getAvatar() : user.getAvatar(),
                profile != null ? profile.getGender() : null,
                region,
                profile != null ? profile.getBio() : null,
                profile != null ? countOrZero(profile.getFollowerCount()) : 0,
                profile != null ? countOrZero(profile.getFollowingCount()) : 0,
                0
        );
    }

    private static int countOrZero(Integer n) {
        return n != null ? n : 0;
    }
}
