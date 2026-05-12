package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.audit.Audit;
import com.blog.personalblogbackend.common.Result;
import com.blog.personalblogbackend.common.web.ClientIp;
import com.blog.personalblogbackend.dto.LoginRequest;
import com.blog.personalblogbackend.dto.auth.CaptchaResponseDto;
import com.blog.personalblogbackend.exception.ServiceException;
import com.blog.personalblogbackend.security.CaptchaService;
import com.blog.personalblogbackend.security.LoginThrottleService;
import com.blog.personalblogbackend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private LoginThrottleService loginThrottleService;

    @GetMapping("/captcha")
    public Result<CaptchaResponseDto> captcha() {
        return Result.success(captchaService.generate());
    }

    @Audit("LOGIN")
    @PostMapping("/login")
    public Result<Map<String, String>> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest http) {
        String ip = ClientIp.resolve(http);
        loginThrottleService.checkIpBurst(ip);
        loginThrottleService.ensureNotLocked(loginRequest.getUsername(), ip);
        captchaService.verify(loginRequest.getCaptchaKey(), loginRequest.getCaptchaCode());
        boolean rememberMe = Boolean.TRUE.equals(loginRequest.getRememberMe());
        try {
            String token = authService.login(
                    loginRequest.getUsername(),
                    loginRequest.getPassword(),
                    rememberMe
            );
            loginThrottleService.clearFailures(loginRequest.getUsername(), ip);
            Map<String, String> data = new HashMap<>();
            data.put("token", token);
            return Result.success(data);
        } catch (ServiceException ex) {
            if (Integer.valueOf(401).equals(ex.getCode())) {
                int remaining = loginThrottleService.recordAuthFailure(loginRequest.getUsername(), ip);
                Map<String, Object> payload = new HashMap<>();
                payload.put("remainingAttempts", remaining);
                throw new ServiceException(401, ex.getMessage(), payload);
            }
            throw ex;
        }
    }
}
