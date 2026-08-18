package com.blog.content.model.vo.knowledge;

import lombok.Data;

import java.util.List;

@Data
public class UserLandscapeVo {
    private List<LandscapeTagVo> nodes;
    private Integer totalArticles;
}
