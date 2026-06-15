package com.blog.content.model.vo.knowledge;

import lombok.Data;

@Data
public class RelatedTagVo {
    private Long tagId;
    private String name;
    private Double weight;
}
