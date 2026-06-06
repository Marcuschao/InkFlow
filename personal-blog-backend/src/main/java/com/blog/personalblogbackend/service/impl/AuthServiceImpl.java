package com.blog.personalblogbackend.service.impl;

import com.blog.personalblogbackend.common.constant.UserRole;
import com.blog.personalblogbackend.config.security.JwtUtils;
import com.blog.personalblogbackend.mapper.UserMapper;
import com.blog.personalblogbackend.model.dto.auth.LoginResult;
import com.blog.personalblogbackend.model.entity.User;
import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.service.AuthService;
import com.blog.personalblogbackend.service.UserService;
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
        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            throw new ServiceException(401, "用户名或密码错误");
        }
        String role = StringUtils.hasText(user.getRole()) ? user.getRole() : UserRole.USER;
        userService.recordLogin(user.getId(), clientIp);
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), role, rememberMe);
        return new LoginResult(token, role);
    }
}
