package com.blog.personalblogbackend.service;

public interface AuthService {
    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return JWT Token
     */
    String login(String username, String password, boolean rememberMe);
}
