package com.blog.content.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("diary")
public class Diary {
    @TableId(type = IdType.AUTO)
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
