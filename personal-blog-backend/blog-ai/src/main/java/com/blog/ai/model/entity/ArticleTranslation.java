package com.blog.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_translation")
public class ArticleTranslation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private String locale;
    private String title;
    private String summary;
    private String content;
    private Integer status;
    private String seoTitle;
    private String seoDescription;
    private LocalDateTime updateTime;
}
