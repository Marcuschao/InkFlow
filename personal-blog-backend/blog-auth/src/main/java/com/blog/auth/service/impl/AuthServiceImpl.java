package com.blog.auth.service.impl;

import com.blog.auth.common.constant.UserRole;
import com.blog.common.security.JwtUtils;
import com.blog.auth.mapper.UserMapper;
import com.blog.auth.model.dto.auth.LoginResult;
import com.blog.auth.model.entity.User;
import com.blog.auth.common.exception.ServiceException;
import com.blog.auth.service.AuthService;
import com.blog.auth.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    @Override
    public LoginResult login(String username, String password, boolean rememberMe, String clientIp) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            throw new ServiceException(401, "用户名或密码错误");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            throw new ServiceException(401, "用户名或密码错误");
        }
        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            throw new ServiceException(401, "用户名或密码错误");
        }
        String role = StringUtils.hasText(user.getRole()) ? user.getRole() : UserRole.USER;
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), role, rememberMe);
        try {
            userService.recordLogin(user.getId(), clientIp);
        } catch (RuntimeException ignored) {
        }
        return new LoginResult(token, role);
    }
}
