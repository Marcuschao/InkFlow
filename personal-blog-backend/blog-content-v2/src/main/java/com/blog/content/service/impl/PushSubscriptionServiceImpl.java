package com.blog.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.content.model.dto.push.PushSubscribeRequest;
import com.blog.content.model.entity.PushSubscription;
import com.blog.content.common.exception.ServiceException;
import com.blog.content.mapper.PushSubscriptionMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PushSubscriptionServiceImpl {

    @Autowired
    private PushSubscriptionMapper pushSubscriptionMapper;

    public void subscribe(PushSubscribeRequest req, Long userId, HttpServletRequest request) {
        if (req == null || !StringUtils.hasText(req.getEndpoint()) || req.getKeys() == null) {
            throw new ServiceException(400, "订阅数据不完整");
        }
        String p256 = req.getKeys().getP256dh();
        String auth = req.getKeys().getAuth();
        if (!StringUtils.hasText(p256) || !StringUtils.hasText(auth)) {
            throw new ServiceException(400, "密钥不完整");
        }
        String ua = request != null ? trimUa(request.getHeader("User-Agent")) : null;
        PushSubscription existing = pushSubscriptionMapper.selectOne(new LambdaQueryWrapper<PushSubscription>()
                .eq(PushSubscription::getEndpoint, req.getEndpoint()));
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setP256dh(p256);
            existing.setAuth(auth);
            existing.setUserId(userId);
            existing.setUserAgent(ua);
            existing.setFailureCount(0);
            existing.setUpdatedAt(now);
            pushSubscriptionMapper.updateById(existing);
            return;
        }
        PushSubscription row = new PushSubscription();
        row.setEndpoint(req.getEndpoint());
        row.setP256dh(p256);
        row.setAuth(auth);
        row.setUserId(userId);
        row.setUserAgent(ua);
        row.setFailureCount(0);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        pushSubscriptionMapper.insert(row);
    }

    public void unsubscribe(String endpoint) {
        if (!StringUtils.hasText(endpoint)) {
            return;
        }
        pushSubscriptionMapper.delete(new LambdaQueryWrapper<PushSubscription>()
                .eq(PushSubscription::getEndpoint, endpoint));
    }

    public long countActive() {
        return pushSubscriptionMapper.selectCount(null);
    }

    public List<PushSubscription> listAll() {
        return pushSubscriptionMapper.selectList(null);
    }

    public void deleteByEndpoint(String endpoint) {
        unsubscribe(endpoint);
    }

    private static String trimUa(String ua) {
        if (!StringUtils.hasText(ua)) {
            return null;
        }
        return ua.length() > 500 ? ua.substring(0, 500) : ua;
    }
}
