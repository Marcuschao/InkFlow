package com.blog.content.model.dto.diary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DiarySaveRequest {
    private LocalDate diaryDate;

    @Size(max = 128)
    private String title;

    @NotBlank
    private String content;

    @NotNull
    private Integer contentType;

    @Size(max = 512)
    private String tags;

    private Boolean isPublic;
}
