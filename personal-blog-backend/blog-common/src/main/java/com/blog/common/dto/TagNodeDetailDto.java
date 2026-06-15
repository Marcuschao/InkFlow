package com.blog.common.dto;

import lombok.Data;

import java.util.List;

@Data
public class TagNodeDetailDto {
    private Long tagId;
    private String name;
    private Integer articleCount;
}
