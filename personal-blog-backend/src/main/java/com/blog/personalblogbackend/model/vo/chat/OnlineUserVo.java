package com.blog.personalblogbackend.model.vo.chat;

import lombok.Data;

@Data
public class OnlineUserVo {
    private Long userId;
    private String username;
    private String avatar;
    private Boolean admin;
}
