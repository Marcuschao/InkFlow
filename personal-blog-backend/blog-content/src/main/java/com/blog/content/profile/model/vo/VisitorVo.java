package com.blog.content.profile.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VisitorVo {
    private Long userId;
    private String nickname;
    private String avatar;
    private LocalDateTime visitTime;
}
