package com.blog.personalblogbackend.service.impl;



import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.blog.personalblogbackend.common.constant.UserRole;

import com.blog.personalblogbackend.common.exception.ServiceException;

import com.blog.personalblogbackend.common.util.IpRegionService;

import com.blog.personalblogbackend.config.security.JwtUtils;

import com.blog.personalblogbackend.mapper.UserMapper;

import com.blog.personalblogbackend.mapper.UserProfileMapper;

import com.blog.personalblogbackend.model.dto.auth.LoginResult;

import com.blog.personalblogbackend.model.dto.auth.RegisterRequest;

import com.blog.personalblogbackend.model.dto.user.UpdateProfileRequest;

import com.blog.personalblogbackend.model.entity.User;

import com.blog.personalblogbackend.model.entity.UserProfile;

import com.blog.personalblogbackend.model.vo.user.PublicUserVo;

import com.blog.personalblogbackend.model.vo.user.UserProfileVo;

import com.blog.personalblogbackend.service.FileStorageService;
import com.blog.personalblogbackend.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.util.StringUtils;



import java.time.LocalDateTime;

import java.util.Collection;

import java.util.Collections;

import java.util.HashMap;

import java.util.List;

import java.util.Map;



@Service

public class UserServiceImpl implements UserService {



    @Autowired

    private UserMapper userMapper;

    @Autowired

    private UserProfileMapper userProfileMapper;

    @Autowired

    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired

    private JwtUtils jwtUtils;

    @Autowired

    private IpRegionService ipRegionService;

    @Autowired

    private FileStorageService fileStorageService;



    @Override

    @Transactional

    public LoginResult register(RegisterRequest request, String clientIp) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {

            throw new ServiceException(400, "两次密码不一致");

        }

        String username = request.getUsername().trim();

        String email = request.getEmail().trim().toLowerCase();

        if (userMapper.selectCount(new QueryWrapper<User>().eq("username", username)) > 0) {

            throw new ServiceException(400, "用户名已被占用");

        }

        if (userMapper.selectCount(new QueryWrapper<User>().eq("email", email)) > 0) {

            throw new ServiceException(400, "邮箱已被注册");

        }

        String ip = StringUtils.hasText(clientIp) ? clientIp.trim() : "";

        String region = ipRegionService.resolve(ip);

        User user = new User();

        user.setUsername(username);

        user.setEmail(email);

        user.setPassword(bCryptPasswordEncoder.encode(request.getPassword()));

        user.setNickname(username);

        user.setRole(UserRole.USER);

        user.setRegisterIp(ip);

        user.setRegisterRegion(region);

        user.setCreateTime(LocalDateTime.now());

        userMapper.insert(user);



        UserProfile profile = new UserProfile();

        profile.setUserId(user.getId());

        profile.setNickname(username);

        profile.setRegion(region);

        profile.setLastLoginIp(ip);

        profile.setLastLoginRegion(region);

        profile.setLastLoginTime(LocalDateTime.now());

        profile.setUpdateTime(LocalDateTime.now());

        userProfileMapper.insert(profile);



        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), UserRole.USER, false);

        return new LoginResult(token, UserRole.USER);

    }



    @Override

    @Transactional

    public void recordLogin(Long userId, String clientIp) {

        UserProfile profile = userProfileMapper.selectById(userId);

        if (profile == null) {

            User u = userMapper.selectById(userId);

            if (u == null) {

                return;

            }

            profile = new UserProfile();

            profile.setUserId(userId);

            profile.setNickname(StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername());

            profile.setAvatar(u.getAvatar());

            userProfileMapper.insert(profile);

        }

        String ip = StringUtils.hasText(clientIp) ? clientIp.trim() : "";

        String region = ipRegionService.resolve(ip);

        profile.setLastLoginIp(ip);

        profile.setLastLoginRegion(region);

        profile.setLastLoginTime(LocalDateTime.now());

        profile.setUpdateTime(LocalDateTime.now());

        userProfileMapper.updateById(profile);

    }



    @Override

    public UserProfileVo getProfile(Long userId) {

        User user = userMapper.selectById(userId);

        if (user == null) {

            throw new ServiceException(404, "用户不存在");

        }

        UserProfile profile = userProfileMapper.selectById(userId);

        return toProfile(user, profile);

    }



    @Override

    @Transactional

    public UserProfileVo updateProfile(Long userId, UpdateProfileRequest request) {

        User user = userMapper.selectById(userId);

        if (user == null) {

            throw new ServiceException(404, "用户不存在");

        }

        UserProfile profile = userProfileMapper.selectById(userId);

        if (profile == null) {

            profile = new UserProfile();

            profile.setUserId(userId);

            profile.setNickname(user.getUsername());

            profile.setUpdateTime(LocalDateTime.now());

            userProfileMapper.insert(profile);

        }

        if (request.getNickname() != null) {

            String nickname = request.getNickname().trim();

            profile.setNickname(StringUtils.hasText(nickname) ? nickname : user.getUsername());

        }

        if (request.getAvatar() != null) {

            profile.setAvatar(StringUtils.hasText(request.getAvatar()) ? request.getAvatar().trim() : null);

        }

        if (request.getGender() != null) {

            profile.setGender(request.getGender());

        }

        if (request.getBio() != null) {

            profile.setBio(StringUtils.hasText(request.getBio()) ? request.getBio().trim() : null);

        }

        profile.setUpdateTime(LocalDateTime.now());

        userProfileMapper.updateById(profile);

        return toProfile(user, profile);

    }

    @Override
    @Transactional
    public UserProfileVo uploadAvatar(Long userId, org.springframework.web.multipart.MultipartFile file) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(404, "用户不存在");
        }
        String url = fileStorageService.saveAvatar(userId, file);
        UserProfile profile = userProfileMapper.selectById(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setNickname(user.getUsername());
            profile.setAvatar(url);
            profile.setUpdateTime(LocalDateTime.now());
            userProfileMapper.insert(profile);
        } else {
            profile.setAvatar(url);
            profile.setUpdateTime(LocalDateTime.now());
            userProfileMapper.updateById(profile);
        }
        return toProfile(user, profile);
    }



    @Override

    public PublicUserVo getPublicProfile(Long userId) {

        User user = userMapper.selectById(userId);

        if (user == null) {

            throw new ServiceException(404, "用户不存在");

        }

        UserProfile profile = userProfileMapper.selectById(userId);

        return toPublic(user, profile);

    }



    @Override

    public Map<Long, UserProfile> mapProfilesByUserIds(Collection<Long> userIds) {

        if (userIds == null || userIds.isEmpty()) {

            return Collections.emptyMap();

        }

        List<UserProfile> list = userProfileMapper.selectList(

                new LambdaQueryWrapper<UserProfile>().in(UserProfile::getUserId, userIds));

        Map<Long, UserProfile> map = new HashMap<>();

        for (UserProfile p : list) {

            map.put(p.getUserId(), p);

        }

        return map;

    }



    static UserProfileVo toProfile(User user, UserProfile profile) {

        String role = StringUtils.hasText(user.getRole()) ? user.getRole() : UserRole.USER;

        String nickname = profile != null && StringUtils.hasText(profile.getNickname())

                ? profile.getNickname()

                : (StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());

        String avatar = profile != null ? profile.getAvatar() : user.getAvatar();

        String region = profile != null && StringUtils.hasText(profile.getRegion())

                ? profile.getRegion()

                : (profile != null ? profile.getLastLoginRegion() : null);

        return new UserProfileVo(

                user.getId(),

                user.getUsername(),

                nickname,

                avatar,

                user.getEmail(),

                role,

                profile != null ? profile.getGender() : null,

                profile != null ? profile.getBio() : null,

                region,

                user.getRegisterRegion(),

                profile != null ? profile.getLastLoginTime() : null,
                profile != null ? countOrZero(profile.getFollowerCount()) : 0,
                profile != null ? countOrZero(profile.getFollowingCount()) : 0
        );
    }

    private static int countOrZero(Integer n) {
        return n != null ? n : 0;
    }

    static PublicUserVo toPublic(User user, UserProfile profile) {

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
                profile != null ? countOrZero(profile.getFollowingCount()) : 0
        );
    }
}


