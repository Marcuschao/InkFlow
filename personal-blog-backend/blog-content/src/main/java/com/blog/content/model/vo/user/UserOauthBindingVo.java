package com.blog.content.model.vo.user;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserOauthBindingVo {
    private String provider;
    private String providerUsername;
    private LocalDateTime bindTime;
}
