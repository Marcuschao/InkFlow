package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.config.audit.Audit;
import com.blog.personalblogbackend.common.support.Result;
import com.blog.personalblogbackend.common.support.web.ClientIp;
import com.blog.personalblogbackend.model.dto.LoginRequest;
import com.blog.personalblogbackend.model.dto.auth.LoginResult;
import com.blog.personalblogbackend.model.dto.auth.RegisterRequest;
import com.blog.personalblogbackend.model.dto.auth.CaptchaResponseDto;
import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.config.security.CaptchaService;
import com.blog.personalblogbackend.config.security.LoginThrottleService;
import com.blog.personalblogbackend.service.AuthService;
import com.blog.personalblogbackend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final CaptchaService captchaService;
    private final LoginThrottleService loginThrottleService;

    @GetMapping("/captcha")
    public Result<CaptchaResponseDto> captcha() {
        return Result.success(captchaService.generate());
    }

    @PostMapping("/register")
    public Result<LoginResult> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        captchaService.verify(request.getCaptchaKey(), request.getCaptchaCode());
        String ip = ClientIp.resolve(http);
        LoginResult result = userService.register(request, ip);
        return Result.success(result);
    }

    @Audit("LOGIN")
    @PostMapping("/login")
    public Result<LoginResult> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest http) {
        String ip = ClientIp.resolve(http);
        loginThrottleService.checkIpBurst(ip);
        loginThrottleService.ensureNotLocked(loginRequest.getUsername(), ip);
        captchaService.verify(loginRequest.getCaptchaKey(), loginRequest.getCaptchaCode());
        boolean rememberMe = Boolean.TRUE.equals(loginRequest.getRememberMe());
        try {
            LoginResult result = authService.login(
                    loginRequest.getUsername(),
                    loginRequest.getPassword(),
                    rememberMe,
                    ip
            );
            loginThrottleService.clearFailures(loginRequest.getUsername(), ip);
            return Result.success(result);
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
