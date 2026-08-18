package com.blog.content.model.vo.notification;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVo {
    private Long id;
    private Long actorUserId;
    private String actorNickname;
    private String actorAvatar;
    private String type;
    private Long targetId;
    private String targetType;
    private String content;
    private Boolean read;
    private LocalDateTime createTime;
}
