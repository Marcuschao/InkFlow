package com.blog.personalblogbackend.hotsearch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotItem {
    private Integer rank;
    private String title;
    private String url;
    private String heat;
    private LocalDateTime updatedAt;
}
