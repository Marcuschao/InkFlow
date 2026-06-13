package com.blog.content.model.dto.push;

import lombok.Data;

@Data
public class PushSubscribeRequest {
    private String endpoint;
    private PushSubscribeRequest.Keys keys;

    @Data
    public static class Keys {
        private String p256dh;
        private String auth;
    }
}
