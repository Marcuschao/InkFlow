package com.blog.content.gamification.badge.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("badge")
public class Badge {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String iconUrl;
    private String triggerCondition;
    private LocalDateTime createTime;
}
