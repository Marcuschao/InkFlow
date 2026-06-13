package com.blog.content.model.dto.push;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VapidPublicResponse {
    private boolean enabled;
    private String publicKey;
    private String subject;
}
