package com.blog.personalblogbackend.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InteractionPersistMessage {
    private String messageId;
    private String action;
    private Long userId;
    private Long articleId;
    private Boolean liked;
    private Boolean favorited;

    public static final String LIKE_TOGGLE = "LIKE_TOGGLE";
    public static final String FAVORITE_TOGGLE = "FAVORITE_TOGGLE";
}
