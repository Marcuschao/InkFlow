package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.common.support.Result;
import com.blog.personalblogbackend.config.security.CurrentUserService;
import com.blog.personalblogbackend.model.dto.user.UpdateProfileRequest;
import com.blog.personalblogbackend.model.vo.user.PublicUserVo;
import com.blog.personalblogbackend.model.vo.user.UserProfileVo;
import com.blog.personalblogbackend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private CurrentUserService currentUserService;

    @GetMapping("/me")
    public Result<UserProfileVo> me() {
        return Result.success(userService.getProfile(currentUserService.requireUserId()));
    }

    @PutMapping("/profile")
    public Result<UserProfileVo> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.success(userService.updateProfile(currentUserService.requireUserId(), request));
    }

    @PostMapping("/avatar")
    public Result<UserProfileVo> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return Result.success(userService.uploadAvatar(currentUserService.requireUserId(), file));
    }

    @GetMapping("/{id}")
    public Result<PublicUserVo> publicProfile(@PathVariable Long id) {
        return Result.success(userService.getPublicProfile(id));
    }
}
