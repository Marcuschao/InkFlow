package com.blog.auth.model.vo.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVo {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String role;
    private Integer gender;
    private String bio;
    private String region;
    private String registerRegion;
    private LocalDateTime lastLoginTime;
    private Integer followerCount;
    private Integer followingCount;
    private Integer points;
}
