package com.blog.content.model.vo.knowledge;

import lombok.Data;

@Data
public class RelatedAuthorVo {
    private Long authorId;
    private String nickname;
    private Integer articleCount;
}
