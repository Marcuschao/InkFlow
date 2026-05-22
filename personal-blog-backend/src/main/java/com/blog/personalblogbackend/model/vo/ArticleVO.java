package com.blog.personalblogbackend.model.vo;

import com.blog.personalblogbackend.model.entity.Article;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArticleVO extends Article {
    private String viewingLocale;
    private Boolean translationActive;
    private Boolean liked;
    private Boolean favorited;
    private String authorNickname;
    private String authorAvatar;
}
