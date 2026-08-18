package com.blog.content.model.vo.diary;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DiaryVo {
    private Long id;
    private Long userId;
    private LocalDate diaryDate;
    private String title;
    private String content;
    private Integer contentType;
    private String tags;
    private Integer isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
