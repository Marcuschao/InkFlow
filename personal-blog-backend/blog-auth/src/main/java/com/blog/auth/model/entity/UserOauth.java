package com.blog.auth.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_oauth")
public class UserOauth {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String provider;
    private String providerUserId;
    private String providerUsername;
    private LocalDateTime bindTime;
}
