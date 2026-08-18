package com.blog.content.gamification.shop.model.vo;

import lombok.Data;

@Data
public class ShopItemVo {
    private Long id;
    private String name;
    private String description;
    private String type;
    private String effectConfig;
    private Integer price;
    private Integer durationDays;
    private String iconUrl;
    private String status;
    private Boolean owned;
    private Boolean affordable;
}
