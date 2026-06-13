package com.blog.content.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("blog_site")
public class BlogSite {
    @TableId
    private Long id;
    private String siteTitle;
    private String siteDescription;
    private String siteUrl;
    private LocalDateTime launchTime;
    private LocalDateTime updateTime;
}
