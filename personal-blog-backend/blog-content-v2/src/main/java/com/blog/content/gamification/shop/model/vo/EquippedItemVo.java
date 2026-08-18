package com.blog.content.gamification.shop.model.vo;

import lombok.Data;

@Data
public class EquippedItemVo {
    private Long userItemId;
    private Long itemId;
    private String name;
    private String type;
    private String effectConfig;
    private String iconUrl;
}
