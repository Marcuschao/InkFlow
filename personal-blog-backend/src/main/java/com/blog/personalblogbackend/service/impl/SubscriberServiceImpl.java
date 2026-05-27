package com.blog.personalblogbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.personalblogbackend.mapper.SubscriberMapper;
import com.blog.personalblogbackend.model.dto.subscribe.SubscribeRequest;
import com.blog.personalblogbackend.model.entity.Subscriber;
import com.blog.personalblogbackend.service.BlogMailService;
import com.blog.personalblogbackend.service.BlogSiteService;
import com.blog.personalblogbackend.service.SubscriberService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SubscriberServiceImpl extends ServiceImpl<SubscriberMapper, Subscriber> implements SubscriberService {

    private final BlogMailService blogMailService;
    private final BlogSiteService blogSiteService;

    public SubscriberServiceImpl(BlogMailService blogMailService, BlogSiteService blogSiteService) {
        this.blogMailService = blogMailService;
        this.blogSiteService = blogSiteService;
    }

    @Override
    public void subscribe(SubscribeRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        Subscriber existing = getOne(new LambdaQueryWrapper<Subscriber>().eq(Subscriber::getEmail, email).last("LIMIT 1"));
        String token;
        if (existing != null) {
            token = UUID.randomUUID().toString().replace("-", "");
            existing.setToken(token);
            existing.setStatus(STATUS_PENDING);
            existing.setCreateTime(LocalDateTime.now());
            updateById(existing);
        } else {
            Subscriber s = new Subscriber();
            s.setEmail(email);
            token = UUID.randomUUID().toString().replace("-", "");
            s.setToken(token);
            s.setStatus(STATUS_PENDING);
            s.setCreateTime(LocalDateTime.now());
            save(s);
        }
        sendConfirmation(email, token);
    }

    @Override
    public boolean confirmSubscribe(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        Subscriber s = getOne(new LambdaQueryWrapper<Subscriber>().eq(Subscriber::getToken, token.trim()).last("LIMIT 1"));
        if (s == null) {
            return false;
        }
        s.setStatus(STATUS_ACTIVE);
        updateById(s);
        return true;
    }

    @Override
    public List<String> listActiveEmails() {
        return list(new LambdaQueryWrapper<Subscriber>().eq(Subscriber::getStatus, STATUS_ACTIVE)).stream()
                .map(Subscriber::getEmail)
                .collect(Collectors.toList());
    }

    private void sendConfirmation(String email, String token) {
        String base = blogSiteService.getSiteUrl().replaceAll("/$", "");
        String url = base + "/api/subscribe/confirm?token=" + token;
        String subject = "[" + blogSiteService.getSiteTitle() + "] 确认订阅";
        String body = "请点击链接确认订阅（如需忽略请勿点开）：\n" + url + "\n";
        blogMailService.sendIfConfigured(email, subject, body);
    }
}
