package com.blog.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatProfileBroadcastRequest {
    private Long userId;
    private String nickname;
    private String avatar;
}
