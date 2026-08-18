package com.blog.content.model.vo.diary;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DiaryPublicDetailVo {
    private Long id;
    private String title;
    private LocalDate diaryDate;
    private String content;
    private Integer contentType;
    private String tags;
    private LocalDateTime createdAt;
}
