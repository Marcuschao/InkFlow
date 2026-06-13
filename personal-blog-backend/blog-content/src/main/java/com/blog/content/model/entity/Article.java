package com.blog.content.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List; // 用于存储文章关联的标签列表

@Data
@TableName("article")
public class Article {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String summary;
    private String seoTitle;
    private String seoDescription;
    private String content; // Markdown内容
    private String cover;
    private Long categoryId;
    private Long authorId;
    private Integer status;
    private String reviewReason;
    private Integer reviewScore;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime submittedAt;
    private Integer freshnessStatus;
    private LocalDateTime freshnessCheckedAt;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    @Version
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private List<Tag> tags;
}
