package com.blog.content.hotsearch.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotSearchListVo {
    private String source;
    private String sourceName;
    private List<HotItem> items;
    private LocalDateTime fetchedAt;
    private boolean fallback;
}
