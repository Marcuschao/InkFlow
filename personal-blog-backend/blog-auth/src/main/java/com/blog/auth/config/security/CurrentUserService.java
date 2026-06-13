package com.blog.auth.config.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blog.auth.model.entity.User;
import com.blog.auth.common.exception.ServiceException;
import com.blog.auth.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    @Autowired
    private UserMapper userMapper;

    public User requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new ServiceException(401, "未登录");
        }
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", auth.getName()));
        if (user == null) {
            throw new ServiceException(401, "用户不存在");
        }
        return user;
    }

    public Long requireUserId() {
        return requireUser().getId();
    }

    public Long optionalUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return null;
        }
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", auth.getName()));
        return user != null ? user.getId() : null;
    }

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}

