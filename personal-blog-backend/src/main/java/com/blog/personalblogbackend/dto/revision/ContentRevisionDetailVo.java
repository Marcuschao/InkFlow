package com.blog.personalblogbackend.dto.revision;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ContentRevisionDetailVo {

    private Long id;
    private Integer revisionNo;
    private String title;
    private String summary;
    private String seoTitle;
    private String seoDescription;
    private String content;
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
