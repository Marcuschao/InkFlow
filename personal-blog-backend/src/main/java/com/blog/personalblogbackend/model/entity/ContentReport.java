package com.blog.personalblogbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content_report")
public class ContentReport {
    public static final String TARGET_ARTICLE = "ARTICLE";
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_HANDLED = 1;
    public static final int STATUS_IGNORED = 2;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String targetType;
    private Long targetId;
    private Long reporterId;
    private String reason;
    private Integer status;
    private Long handlerId;
    private String handleNote;
    private LocalDateTime createTime;
    private LocalDateTime handleTime;
}
