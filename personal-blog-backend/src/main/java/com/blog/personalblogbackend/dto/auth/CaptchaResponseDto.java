package com.blog.personalblogbackend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponseDto {
    private String captchaKey;
    private String imageBase64;
}
