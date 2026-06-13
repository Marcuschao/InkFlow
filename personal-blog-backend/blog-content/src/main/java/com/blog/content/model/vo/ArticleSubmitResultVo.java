package com.blog.content.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleSubmitResultVo {
    private Long id;
    private Integer status;
    private String message;
    private String reviewReason;
    private Integer reviewScore;
}
