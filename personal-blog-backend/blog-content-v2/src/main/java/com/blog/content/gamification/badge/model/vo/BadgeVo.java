package com.blog.content.gamification.badge.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BadgeVo {
    private Long id;
    private String name;
    private String description;
    private String iconUrl;
    private String triggerCondition;
    private boolean earned;
    private LocalDateTime awardedTime;
}
