package com.blog.content.gamification.reward.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_reward")
public class ArticleReward {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long fromUserId;
    private Long toUserId;
    private Integer points;
    private LocalDateTime createTime;
}
