package com.blog.common.dto;

import lombok.Data;

@Data
public class UserPublicDto {
    private Long id;
    private String nickname;
    private String avatar;
    private Integer gender;
    private String region;
    private String bio;
    private Integer followerCount;
    private Integer followingCount;
}
