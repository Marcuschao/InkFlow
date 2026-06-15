package com.blog.content.model.vo.knowledge;

import com.blog.content.model.entity.Article;
import lombok.Data;

import java.util.List;

@Data
public class TagNodeDetailVo {
    private Long tagId;
    private String name;
    private Integer articleCount;
    private List<Article> articles;
    private List<RelatedTagVo> relatedTags;
    private List<RelatedAuthorVo> hotAuthors;
    private Boolean subscribed;
}
