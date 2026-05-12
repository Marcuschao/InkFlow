package com.blog.personalblogbackend.security;

import com.blog.personalblogbackend.dto.auth.CaptchaResponseDto;
import com.blog.personalblogbackend.exception.ServiceException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wf.captcha.SpecCaptcha;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
public class CaptchaService {

    private final Cache<String, String> answers = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(2))
            .maximumSize(10_000)
            .build();

    public CaptchaResponseDto generate() {
        SpecCaptcha captcha = new SpecCaptcha(130, 48, 5);
        String plain = captcha.text().toLowerCase(Locale.ROOT);
        String key = UUID.randomUUID().toString().replace("-", "");
        answers.put(key, plain);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        captcha.out(os);
        String b64 = Base64.getEncoder().encodeToString(os.toByteArray());
        return new CaptchaResponseDto(key, b64);
    }

    public void verify(String captchaKey, String captchaCode) {
        if (!StringUtils.hasText(captchaKey) || !StringUtils.hasText(captchaCode)) {
            throw new ServiceException(400, "请输入验证码");
        }
        String expected = answers.getIfPresent(captchaKey);
        answers.invalidate(captchaKey);
        if (expected == null) {
            throw new ServiceException(400, "验证码已失效，请刷新");
        }
        String actual = captchaCode.trim().toLowerCase(Locale.ROOT);
        if (!expected.equals(actual)) {
            throw new ServiceException(400, "验证码错误");
        }
    }
}
