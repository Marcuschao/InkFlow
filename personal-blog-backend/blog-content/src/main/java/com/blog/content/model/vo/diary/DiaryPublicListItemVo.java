package com.blog.content.model.vo.diary;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DiaryPublicListItemVo {
    private Long id;
    private String title;
    private LocalDate diaryDate;
    private String excerpt;
    private String tags;
    private LocalDateTime createdAt;
}
