package com.blog.ai.model.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSourceDto {
    private Long id;
    private String title;
    private String chunkId;
    private Integer ordinal;
    private String snippet;
    private Double score;
    private String link;

    public ChatSourceDto(Long id, String title) {
        this.id = id;
        this.title = title;
    }
}
