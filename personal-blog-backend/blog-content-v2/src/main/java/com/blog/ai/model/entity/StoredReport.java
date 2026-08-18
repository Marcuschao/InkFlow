package com.blog.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stored_report")
public class StoredReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String reportType;
    private Long targetId;
    private String objectKey;
    private String title;
    private Long fileSize;
    private LocalDateTime createdAt;
}
