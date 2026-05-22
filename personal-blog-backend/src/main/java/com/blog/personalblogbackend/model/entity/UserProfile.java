package com.blog.personalblogbackend.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_profile")
public class UserProfile {

    @TableId
    private Long userId;
    private String nickname;
    private String avatar;
    private Integer gender;
    private String bio;
    private String region;
    private Integer followerCount;
    private Integer followingCount;
    private String lastLoginIp;
    private String lastLoginRegion;
    private LocalDateTime lastLoginTime;
    private LocalDateTime updateTime;
}
