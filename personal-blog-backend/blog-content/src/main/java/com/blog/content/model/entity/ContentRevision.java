package com.blog.content.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("content_revision")
public class ContentRevision {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String targetType;
    private Long targetId;
    private Integer revisionNo;

    private String title;
    private String summary;
    private String seoTitle;
    private String seoDescription;
    private String content;
    private String contentStorageKey;
    private String contentMd5;

    private String articleTags;
    private Long articleCategoryId;
    private Integer articleStatus;
    private String articleCover;

    private LocalDate diaryDate;
    private String diaryTags;
    private Integer diaryContentType;
    private Integer diaryIsPublic;

    private String remark;
    private LocalDateTime createdAt;
}
