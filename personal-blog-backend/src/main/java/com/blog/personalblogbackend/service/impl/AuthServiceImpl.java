package com.blog.personalblogbackend.service.impl;

import com.blog.personalblogbackend.common.JwtUtils;
import com.blog.personalblogbackend.mapper.UserMapper;
import com.blog.personalblogbackend.entity.User;
import com.blog.personalblogbackend.exception.ServiceException;
import com.blog.personalblogbackend.service.AuthService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public String login(String username, String password, boolean rememberMe) {
        // 1. 根据用户名查询用户
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            throw new ServiceException(401, "用户名或密码错误");
        }

        // 2. 验证密码
        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            throw new ServiceException(401, "用户名或密码错误");
        }

        // 3. 生成 JWT Token
        return jwtUtils.generateToken(user.getId(), user.getUsername(), rememberMe);
    }
}
