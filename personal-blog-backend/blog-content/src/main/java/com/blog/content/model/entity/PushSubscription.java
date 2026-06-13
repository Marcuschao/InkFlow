package com.blog.content.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("push_subscription")
public class PushSubscription {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String endpoint;
    private String p256dh;
    private String auth;
    private Long userId;
    private String userAgent;
    private Integer failureCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
