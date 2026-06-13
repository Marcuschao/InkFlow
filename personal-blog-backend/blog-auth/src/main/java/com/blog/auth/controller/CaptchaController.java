package com.blog.auth.controller;

import com.blog.auth.captcha.MathCaptchaStore;
import com.blog.auth.common.support.Result;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    @Autowired
    private MathCaptchaStore mathCaptchaStore;

    @GetMapping("/math")
    public Result<CaptchaDto> math() {
        MathCaptchaStore.Challenge c = mathCaptchaStore.nextChallenge();
        return Result.success(new CaptchaDto(c.captchaId(), c.question()));
    }

    @Data
    @AllArgsConstructor
    public static class CaptchaDto {
        private String captchaId;
        private String question;
    }
}
