package com.blog.content.model.dto.knowledge;

import lombok.Data;

import java.util.List;

@Data
public class UserLandscapeRequest {
    private List<Long> recentArticleIds;
}
