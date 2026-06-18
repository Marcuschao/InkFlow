package com.blog.content.gamification.reward.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArticleRewardRequest {
    @NotNull(message = "积分不能为空")
    private Integer points;
}
