package com.blog.content.knowledge.model;

import lombok.Data;

@Data
public class TagCooccurrenceDto {
    private Long tagId1;
    private Long tagId2;
    private Long cooccurCount;
    private Double avgDays;
}
