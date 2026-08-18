package com.blog.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_quota_whitelist")
public class AiQuotaWhitelist {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String remark;
    private LocalDateTime createdAt;
}
