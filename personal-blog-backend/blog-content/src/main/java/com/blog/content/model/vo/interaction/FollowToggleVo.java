package com.blog.content.model.vo.interaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowToggleVo {
    private Boolean following;
    private Integer followerCount;
    private Integer followingCount;
}
