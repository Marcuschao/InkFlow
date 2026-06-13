package com.blog.content.model.vo.interaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBriefVo {
    private Long id;
    private String nickname;
    private String avatar;
    private Boolean following;
}
