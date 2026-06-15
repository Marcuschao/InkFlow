package com.blog.content.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_tag_score")
public class ArticleTagScore {
    private Long articleId;
    private Long tagId;
    private Double score;
    private LocalDateTime updateTime;
}
