package com.blog.auth.controller;

import com.blog.auth.common.support.Result;
import com.blog.auth.config.security.CurrentUserService;
import com.blog.auth.model.vo.user.UserOauthBindingVo;
import com.blog.auth.service.UserOAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/me/oauth")
public class UserOAuthController {

    @Autowired
    private UserOAuthService userOAuthService;

    @Autowired
    private CurrentUserService currentUserService;

    @GetMapping
    public Result<List<UserOauthBindingVo>> list() {
        return Result.success(userOAuthService.listBindings(currentUserService.requireUserId()));
    }

    @PostMapping("/github/bind")
    public Result<Map<String, String>> bindGithub(HttpServletResponse response) {
        Long userId = currentUserService.requireUserId();
        String token = userOAuthService.createBindToken(userId);
        Cookie cookie = new Cookie("OAUTH_BIND", token);
        cookie.setPath("/");
        cookie.setMaxAge(600);
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
        return Result.success(Map.of("authorizeUrl", "/oauth2/authorization/github"));
    }

    @DeleteMapping("/github")
    public Result<Void> unbindGithub() {
        userOAuthService.unbindGithub(currentUserService.requireUserId());
        return Result.success(null);
    }
}
