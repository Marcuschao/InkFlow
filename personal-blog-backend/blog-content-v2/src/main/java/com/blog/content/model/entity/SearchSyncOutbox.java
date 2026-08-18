package com.blog.content.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("search_sync_outbox")
public class SearchSyncOutbox {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private String eventType;
    private LocalDateTime articleUpdatedAt;
    private String status;
    private Integer retryCount;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
