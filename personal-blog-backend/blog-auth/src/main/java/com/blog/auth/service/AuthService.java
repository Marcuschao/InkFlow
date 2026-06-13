package com.blog.auth.service;

import com.blog.auth.model.dto.auth.LoginResult;

public interface AuthService {

    LoginResult login(String username, String password, boolean rememberMe, String clientIp);
}
