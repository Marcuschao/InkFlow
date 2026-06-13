package com.blog.common.dto;

import lombok.Data;

@Data
public class UserInternalDto {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private String role;
}
