package com.blog.content.gamification.shop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_item")
public class UserItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long itemId;
    private String status;
    private LocalDateTime obtainTime;
    private LocalDateTime expireTime;
    private LocalDateTime usedTime;
}
