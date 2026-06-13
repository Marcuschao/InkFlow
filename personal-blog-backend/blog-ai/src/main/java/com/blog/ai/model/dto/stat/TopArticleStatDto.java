package com.blog.ai.model.dto.stat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopArticleStatDto {
    private Long id;
    private String title;
    private Integer viewCount;
}
