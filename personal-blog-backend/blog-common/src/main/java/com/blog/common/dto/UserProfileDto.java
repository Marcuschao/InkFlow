package com.blog.common.dto;

import lombok.Data;

@Data
public class UserProfileDto {
    private Long userId;
    private String nickname;
    private String avatar;
    private Integer gender;
    private String region;
    private String bio;
}
