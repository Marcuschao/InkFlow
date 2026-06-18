package com.blog.content.gamification.reward.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleRewardVo {
    private Long id;
    private Long articleId;
    private Long fromUserId;
    private Long toUserId;
    private Integer points;
    private LocalDateTime createTime;
    private String fromNickname;
    private String fromAvatar;
    private String toNickname;
    private String toAvatar;
    private String articleTitle;
    private Boolean received;
}
