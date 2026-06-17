package com.blog.content.profile.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TimelineItemVo {
    private String actionType;
    private Long actorId;
    private String actorName;
    private String actorAvatar;
    private Long articleId;
    private String articleTitle;
    private LocalDateTime eventTime;
}
