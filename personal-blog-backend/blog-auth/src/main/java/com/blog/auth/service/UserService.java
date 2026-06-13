package com.blog.auth.service;

import com.blog.auth.model.dto.auth.LoginResult;
import com.blog.auth.model.dto.auth.RegisterRequest;
import com.blog.auth.model.dto.user.UpdateProfileRequest;
import com.blog.auth.model.entity.UserProfile;
import com.blog.auth.model.vo.user.PublicUserVo;
import com.blog.auth.model.vo.user.UserProfileVo;

import java.util.Collection;
import java.util.Map;

public interface UserService {

    /**
     * 注册用户
     *
     * @param request
     * @param clientIp
     * @return
     */
    LoginResult register(RegisterRequest request, String clientIp);

    /**
     * 记录登录信息
     *
     * @param userId
     * @param clientIp
     */
    void recordLogin(Long userId, String clientIp);

    /**
     * 获取用户个人资料
     *
     * @param userId
     * @return
     */
    UserProfileVo getProfile(Long userId);

    /**
     * 更新用户个人资料
     *
     * @param userId
     * @param request
     * @return
     */
    UserProfileVo updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * 上传用户头像
     *
     * @param userId
     * @param file
     * @return
     */
    UserProfileVo uploadAvatar(Long userId, org.springframework.web.multipart.MultipartFile file);

    /**
     * 获取用户公开资料
     *
     * @param userId
     * @return
     */
    PublicUserVo getPublicProfile(Long userId);

    /**
     * 根据用户ID批量获取用户资料
     *
     * @param userIds
     * @return
     */
    Map<Long, UserProfile> mapProfilesByUserIds(Collection<Long> userIds);
}
