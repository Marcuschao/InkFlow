package com.blog.content.gamification.shop.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserItemVo {
    private Long id;
    private Long userId;
    private Long itemId;
    private String status;
    private LocalDateTime obtainTime;
    private LocalDateTime expireTime;
    private LocalDateTime usedTime;
    private String name;
    private String description;
    private String type;
    private String effectConfig;
    private Integer price;
    private Integer durationDays;
    private String iconUrl;
}
