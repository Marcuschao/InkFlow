package com.blog.personalblogbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.personalblogbackend.common.constant.UserRole;
import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.config.security.oauth.BlogOAuth2User;
import com.blog.personalblogbackend.mapper.UserMapper;
import com.blog.personalblogbackend.mapper.UserOauthMapper;
import com.blog.personalblogbackend.mapper.UserProfileMapper;
import com.blog.personalblogbackend.model.entity.User;
import com.blog.personalblogbackend.model.entity.UserOauth;
import com.blog.personalblogbackend.model.entity.UserProfile;
import com.blog.personalblogbackend.model.vo.user.UserOauthBindingVo;
import com.blog.personalblogbackend.service.UserOAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserOAuthServiceImpl implements UserOAuthService {

    public static final String PROVIDER_GITHUB = "github";
    private static final String BIND_KEY_PREFIX = "oauth:bind:";

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserOauthMapper userOauthMapper;
    @Autowired
    private UserProfileMapper userProfileMapper;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private StringRedisTemplate redis;
    @Override
    @Transactional
    public BlogOAuth2User resolveGithubUser(Map<String, Object> attributes, Long bindUserId) {
        String providerUserId = stringAttr(attributes, "id");
        if (!StringUtils.hasText(providerUserId)) {
            throw new ServiceException(400, "无法获取 GitHub 用户标识");
        }
        String login = stringAttr(attributes, "login");
        String name = stringAttr(attributes, "name");
        String avatar = stringAttr(attributes, "avatar_url");
        String email = stringAttr(attributes, "email");

        if (bindUserId != null) {
            return linkGithubToUser(bindUserId, providerUserId, login, attributes);
        }

        UserOauth existing = userOauthMapper.selectOne(new LambdaQueryWrapper<UserOauth>()
                .eq(UserOauth::getProvider, PROVIDER_GITHUB)
                .eq(UserOauth::getProviderUserId, providerUserId));
        if (existing != null) {
            User user = userMapper.selectById(existing.getUserId());
            if (user == null) {
                throw new ServiceException(500, "绑定用户不存在");
            }
            return toPrincipal(user, attributes);
        }

        User user = createUserFromGithub(providerUserId, login, name, avatar, email);
        insertOauthBinding(user.getId(), providerUserId, login);
        return toPrincipal(user, attributes);
    }

    private BlogOAuth2User linkGithubToUser(Long userId, String providerUserId, String login, Map<String, Object> attributes) {
        UserOauth taken = userOauthMapper.selectOne(new LambdaQueryWrapper<UserOauth>()
                .eq(UserOauth::getProvider, PROVIDER_GITHUB)
                .eq(UserOauth::getProviderUserId, providerUserId));
        if (taken != null && !taken.getUserId().equals(userId)) {
            throw new ServiceException(400, "该 GitHub 账号已绑定其他用户");
        }
        UserOauth mine = userOauthMapper.selectOne(new LambdaQueryWrapper<UserOauth>()
                .eq(UserOauth::getUserId, userId)
                .eq(UserOauth::getProvider, PROVIDER_GITHUB));
        if (mine == null) {
            insertOauthBinding(userId, providerUserId, login);
        } else {
            mine.setProviderUserId(providerUserId);
            mine.setProviderUsername(login);
            mine.setBindTime(LocalDateTime.now());
            userOauthMapper.updateById(mine);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(404, "用户不存在");
        }
        return toPrincipal(user, attributes);
    }

    private User createUserFromGithub(String providerUserId, String login, String name, String avatar, String email) {
        String baseUsername = StringUtils.hasText(login) ? login.trim() : "github_" + providerUserId;
        String username = uniqueUsername(baseUsername);

        User user = new User();
        user.setUsername(username);
        user.setEmail(StringUtils.hasText(email) ? email.trim().toLowerCase() : null);
        user.setPassword(bCryptPasswordEncoder.encode(randomPassword()));
        user.setPasswordLoginEnabled(false);
        user.setNickname(StringUtils.hasText(name) ? name.trim() : username);
        user.setAvatar(StringUtils.hasText(avatar) ? avatar.trim() : null);
        user.setRole(UserRole.USER);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setNickname(user.getNickname());
        profile.setAvatar(user.getAvatar());
        profile.setUpdateTime(LocalDateTime.now());
        userProfileMapper.insert(profile);
        return user;
    }

    private void insertOauthBinding(Long userId, String providerUserId, String login) {
        UserOauth row = new UserOauth();
        row.setUserId(userId);
        row.setProvider(PROVIDER_GITHUB);
        row.setProviderUserId(providerUserId);
        row.setProviderUsername(login);
        row.setBindTime(LocalDateTime.now());
        userOauthMapper.insert(row);
    }

    private String uniqueUsername(String base) {
        String candidate = base;
        int suffix = 0;
        while (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, candidate)) > 0) {
            suffix++;
            candidate = base + "_" + suffix;
        }
        return candidate;
    }

    private static String randomPassword() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static BlogOAuth2User toPrincipal(User user, Map<String, Object> attributes) {
        return new BlogOAuth2User(user.getId(), user.getUsername(), user.getRole(), attributes);
    }

    private static String stringAttr(Map<String, Object> attributes, String key) {
        Object v = attributes.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number number) {
            return String.valueOf(number.longValue());
        }
        return String.valueOf(v);
    }

    @Override
    public List<UserOauthBindingVo> listBindings(Long userId) {
        return userOauthMapper.selectList(new LambdaQueryWrapper<UserOauth>().eq(UserOauth::getUserId, userId))
                .stream()
                .map(row -> {
                    UserOauthBindingVo vo = new UserOauthBindingVo();
                    vo.setProvider(row.getProvider());
                    vo.setProviderUsername(row.getProviderUsername());
                    vo.setBindTime(row.getBindTime());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public String createBindToken(Long userId) {
        String token = UUID.randomUUID().toString();
        redis.opsForValue().set(BIND_KEY_PREFIX + token, String.valueOf(userId), Duration.ofMinutes(10));
        return token;
    }

    @Override
    public Long consumeBindToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        String key = BIND_KEY_PREFIX + token.trim();
        String userId = redis.opsForValue().get(key);
        if (userId != null) {
            redis.delete(key);
            return Long.parseLong(userId);
        }
        return null;
    }

    @Override
    @Transactional
    public void unbindGithub(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(404, "用户不存在");
        }
        if (!Boolean.TRUE.equals(user.getPasswordLoginEnabled())) {
            throw new ServiceException(400, "仅 OAuth 登录的账号请先设置本地密码后再解绑");
        }
        userOauthMapper.delete(new LambdaQueryWrapper<UserOauth>()
                .eq(UserOauth::getUserId, userId)
                .eq(UserOauth::getProvider, PROVIDER_GITHUB));
    }
}
