package com.blog.content.profile.model.vo;

import com.blog.content.gamification.badge.model.vo.BadgeVo;
import com.blog.content.gamification.sign.model.vo.SignStatusVo;
import lombok.Data;

import java.util.List;

@Data
public class SocialCardVo {
    private Integer points;
    private List<BadgeVo> badges;
    private SignStatusVo signStatus;
    private List<VisitorVo> recentVisitors;
    private List<TimelineItemVo> timelinePreview;
}
