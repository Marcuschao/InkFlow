package com.blog.content.gamification.badge.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BadgeListVo {
    private List<BadgeVo> badges;
    private List<Long> earnedBadgeIds;
}
