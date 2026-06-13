package com.blog.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("page_view_event")
public class PageViewEvent {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String pageType;
    private Long articleId;
    private String visitorId;
    private LocalDateTime createdAt;
}
