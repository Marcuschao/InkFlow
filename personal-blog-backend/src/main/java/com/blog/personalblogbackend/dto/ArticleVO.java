package com.blog.personalblogbackend.dto;

import com.blog.personalblogbackend.entity.Article;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArticleVO extends Article {
    private String viewingLocale;
    private Boolean translationActive;
}
