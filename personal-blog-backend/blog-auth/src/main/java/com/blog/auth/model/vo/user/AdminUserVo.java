package com.blog.auth.model.vo.user;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserVo {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    private String role;
    private Boolean passwordLoginEnabled;
    private String registerIp;
    private String registerRegion;
    private LocalDateTime createTime;
    private String lastLoginIp;
    private String lastLoginRegion;
    private LocalDateTime lastLoginTime;
}
