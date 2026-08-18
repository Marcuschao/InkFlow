package com.blog.content.knowledge.model;

import lombok.Data;

@Data
public class TagWeightRowDto {
    private Long tagId;
    private String name;
    private Double weight;
    private String source;
}
