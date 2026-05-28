package com.blog.personalblogbackend.service;

import com.blog.personalblogbackend.model.dto.auth.LoginResult;
import com.blog.personalblogbackend.model.dto.auth.RegisterRequest;
import com.blog.personalblogbackend.model.dto.user.UpdateProfileRequest;
import com.blog.personalblogbackend.model.vo.user.PublicUserVo;
import com.blog.personalblogbackend.model.vo.user.UserProfileVo;

import java.util.Collection;
import java.util.Map;

public interface UserService {

    LoginResult register(RegisterRequest request, String clientIp);

    void recordLogin(Long userId, String clientIp);

    UserProfileVo getProfile(Long userId);

    UserProfileVo updateProfile(Long userId, UpdateProfileRequest request);

    UserProfileVo uploadAvatar(Long userId, org.springframework.web.multipart.MultipartFile file);

    PublicUserVo getPublicProfile(Long userId);

    Map<Long, com.blog.personalblogbackend.model.entity.UserProfile> mapProfilesByUserIds(Collection<Long> userIds);
}
