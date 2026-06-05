package com.blog.personalblogbackend.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchSyncMessage {
    private String messageId;
    private Long articleId;
    private SearchSyncEventType eventType;
    private LocalDateTime articleUpdatedAt;
}
