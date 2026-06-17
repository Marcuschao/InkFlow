package com.blog.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.auth.common.exception.ServiceException;
import com.blog.auth.config.properties.BlogSiteProperties;
import com.blog.auth.config.properties.NotificationRabbitProperties;
import com.blog.auth.mapper.UserMapper;
import com.blog.auth.model.dto.mail.PasswordResetMailMessage;
import com.blog.auth.model.entity.User;
import com.blog.auth.service.AuditLogQueryService;
import com.blog.auth.service.MailService;
import com.blog.auth.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final Duration RATE_TTL = Duration.ofHours(1);
    private static final int RATE_LIMIT = 3;
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,64}$");

    private final StringRedisTemplate redis;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;
    private final NotificationRabbitProperties notificationProperties;
    private final BlogSiteProperties blogSiteProperties;
    private final MailService mailService;
    private final AuditLogQueryService auditLogQueryService;

    @Override
    public void requestReset(String email, String ip) {
        String normalizedEmail = normalizeEmail(email);
        checkRateLimit("password_reset:rate:email:" + normalizedEmail);
        checkRateLimit("password_reset:rate:ip:" + safeIp(ip));

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, normalizedEmail)
                .last("LIMIT 1"));
        if (user == null) {
            auditLogQueryService.record(normalizedEmail, "PASSWORD_RESET_REQUEST", "exists=false", ip);
            return;
        }

        String token = generateToken(user.getId());
        redis.opsForValue().set(tokenKey(token), String.valueOf(user.getId()), TOKEN_TTL);

        PasswordResetMailMessage message = new PasswordResetMailMessage();
        message.setType("RESET_REQUEST");
        message.setEmail(user.getEmail());
        message.setUsername(user.getUsername());
        message.setResetLink(buildResetLink(token));
        sendMail(NotificationRabbitProperties.RK_PASSWORD_RESET_REQUEST, message);
        auditLogQueryService.record(user.getUsername(), "PASSWORD_RESET_REQUEST", "exists=true", ip);
    }

    @Override
    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        return Boolean.TRUE.equals(redis.hasKey(tokenKey(token.trim())));
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword, String ip) {
        if (!PASSWORD_PATTERN.matcher(String.valueOf(newPassword)).matches()) {
            throw new ServiceException(400, "密码需为 8-64 位且包含字母和数字");
        }
        String rawToken = StringUtils.hasText(token) ? token.trim() : "";
        String key = tokenKey(rawToken);
        String userId = redis.opsForValue().getAndDelete(key);
        if (!StringUtils.hasText(userId)) {
            auditLogQueryService.record("anonymous", "PASSWORD_RESET_FAILED", "reason=invalid_token", ip);
            throw new ServiceException(400, "重置链接已失效");
        }

        User user = userMapper.selectById(Long.valueOf(userId));
        if (user == null) {
            auditLogQueryService.record("anonymous", "PASSWORD_RESET_FAILED", "reason=user_missing", ip);
            throw new ServiceException(400, "重置链接已失效");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordLoginEnabled(true);
        userMapper.updateById(user);

        PasswordResetMailMessage message = new PasswordResetMailMessage();
        message.setType("RESET_SUCCESS");
        message.setEmail(user.getEmail());
        message.setUsername(user.getUsername());
        sendMail(NotificationRabbitProperties.RK_PASSWORD_RESET_SUCCESS, message);
        auditLogQueryService.record(user.getUsername(), "PASSWORD_RESET_SUCCESS", "userId=" + user.getId(), ip);
    }

    private void checkRateLimit(String key) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, RATE_TTL);
        }
        if (count != null && count > RATE_LIMIT) {
            throw new ServiceException(429, "请求过于频繁，请稍后再试");
        }
    }

    private void sendMail(String routingKey, PasswordResetMailMessage message) {
        if (!notificationProperties.isEnabled()) {
            sendDirect(message);
            return;
        }
        try {
            rabbitTemplate.convertAndSend(notificationProperties.getExchange(), routingKey, message);
        } catch (AmqpException ex) {
            sendDirect(message);
        }
    }

    private void sendDirect(PasswordResetMailMessage message) {
        if ("RESET_REQUEST".equals(message.getType())) {
            mailService.sendPasswordResetLink(message.getEmail(), message.getUsername(), message.getResetLink());
            return;
        }
        if ("RESET_SUCCESS".equals(message.getType())) {
            mailService.sendPasswordResetSuccess(message.getEmail(), message.getUsername());
        }
    }

    private String buildResetLink(String token) {
        String configuredBase = StringUtils.hasText(blogSiteProperties.getFrontendBaseUrl())
                ? blogSiteProperties.getFrontendBaseUrl()
                : blogSiteProperties.getSiteUrl();
        String base = StringUtils.hasText(configuredBase)
                ? configuredBase.replaceAll("/+$", "")
                : "";
        String path = normalizePath(blogSiteProperties.getSiteBasePath()) + "/reset-password";
        return base + path + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private static String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ServiceException(400, "邮箱不能为空");
        }
        return email.trim().toLowerCase();
    }

    private static String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        String p = path.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return p.replaceAll("/+$", "");
    }

    private static String tokenKey(String token) {
        return "reset_token:" + token;
    }

    private static String generateToken(Long userId) {
        String uuid = UUID.randomUUID().toString();
        String hash = sha256(uuid + ":" + userId + ":" + System.nanoTime());
        return uuid + "." + hash;
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String safeIp(String ip) {
        return StringUtils.hasText(ip) ? ip.trim() : "unknown";
    }
}
