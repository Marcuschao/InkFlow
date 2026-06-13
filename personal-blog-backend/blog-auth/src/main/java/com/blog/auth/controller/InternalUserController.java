package com.blog.auth.controller;

import com.blog.auth.model.entity.User;
import com.blog.auth.model.entity.UserProfile;
import com.blog.auth.model.vo.user.PublicUserVo;
import com.blog.auth.service.UserService;
import com.blog.common.dto.UserBatchRequest;
import com.blog.common.dto.UserInternalDto;
import com.blog.common.dto.UserProfileDto;
import com.blog.common.dto.UserPublicDto;
import com.blog.common.support.Result;
import com.blog.auth.mapper.UserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final UserMapper userMapper;
    private final UserService userService;

    public InternalUserController(UserMapper userMapper, UserService userService) {
        this.userMapper = userMapper;
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public Result<UserInternalDto> getUser(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }
        UserInternalDto dto = new UserInternalDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole());
        return Result.success(dto);
    }

    @PostMapping("/batch")
    public Result<Map<Long, UserProfileDto>> batchProfiles(@RequestBody UserBatchRequest request) {
        Map<Long, UserProfile> profiles = userService.mapProfilesByUserIds(request.getUserIds());
        Map<Long, UserProfileDto> result = new HashMap<>();
        profiles.forEach((k, v) -> {
            UserProfileDto dto = new UserProfileDto();
            dto.setUserId(v.getUserId());
            dto.setNickname(v.getNickname());
            dto.setAvatar(v.getAvatar());
            dto.setGender(v.getGender());
            dto.setRegion(v.getRegion());
            dto.setBio(v.getBio());
            result.put(k, dto);
        });
        return Result.success(result);
    }

    @GetMapping("/{id}/public")
    public Result<UserPublicDto> getPublic(@PathVariable Long id) {
        PublicUserVo vo = userService.getPublicProfile(id);
        UserPublicDto dto = new UserPublicDto();
        dto.setId(vo.getId());
        dto.setNickname(vo.getNickname());
        dto.setAvatar(vo.getAvatar());
        dto.setGender(vo.getGender());
        dto.setRegion(vo.getRegion());
        dto.setBio(vo.getBio());
        dto.setFollowerCount(vo.getFollowerCount());
        dto.setFollowingCount(vo.getFollowingCount());
        return Result.success(dto);
    }
}
