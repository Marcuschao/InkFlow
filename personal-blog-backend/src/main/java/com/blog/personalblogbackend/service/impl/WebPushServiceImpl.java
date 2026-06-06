package com.blog.personalblogbackend.service.impl;

import com.blog.personalblogbackend.config.properties.BlogSiteProperties;
import com.blog.personalblogbackend.model.entity.Article;
import com.blog.personalblogbackend.model.entity.PushSubscription;
import com.blog.personalblogbackend.common.exception.ServiceException;
import com.blog.personalblogbackend.mapper.ArticleMapper;
import com.blog.personalblogbackend.service.BlogSiteService;
import com.blog.personalblogbackend.service.WebPushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebPushServiceImpl implements WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushServiceImpl.class);

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @Autowired
    private BlogSiteProperties blogSiteProperties;

    @Autowired
    private BlogSiteService blogSiteService;

    @Autowired
    private PushSubscriptionServiceImpl pushSubscriptionService;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private PushService pushService;

    @Override
    public boolean isOperational() {
        ensurePushService();
        BlogSiteProperties.PushSettings p = blogSiteProperties.getPush();
        return p.isEnabled() && pushService != null;
    }

    private synchronized void ensurePushService() {
        if (pushService != null) {
            return;
        }
        BlogSiteProperties.PushSettings p = blogSiteProperties.getPush();
        if (!p.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(p.getPublicKey()) || !StringUtils.hasText(p.getPrivateKey())) {
            log.warn("[push] enabled but missing public/private VAPID keys");
            return;
        }
        try {
            String subject = StringUtils.hasText(p.getSubject()) ? p.getSubject() : "mailto:admin@localhost";
            this.pushService = new PushService(p.getPublicKey(), p.getPrivateKey(), subject);
        } catch (GeneralSecurityException e) {
            log.warn("[push] invalid VAPID keys: {}", e.toString());
        }
    }

    @Override
    public void notifyNewArticle(Long articleId, String articleTitle) {
        ensurePushService();
        BlogSiteProperties.PushSettings p = blogSiteProperties.getPush();
        if (!p.isEnabled() || pushService == null) {
            return;
        }
        String url = blogSiteService.resolvePublicUrl("/article/" + articleId);
        String siteTitle = blogSiteService.getSiteTitle();
        String title = "[" + siteTitle + "] 新文章：" + (articleTitle != null ? articleTitle : "");
        String body = articleTitle != null ? ("《" + articleTitle + "》") : "新文章已发布";
        sendPayload(title, body, url, articleId);
    }

    @Override
    public void sendCustom(String title, String body, String url) {
        ensurePushService();
        if (!blogSiteProperties.getPush().isEnabled() || pushService == null) {
            return;
        }
        sendPayload(title, body, url, null);
    }

    @Override
    public void sendForArticle(Long articleId) {
        Article a = articleMapper.selectById(articleId);
        if (a == null) {
            throw new ServiceException(404, "文章不存在");
        }
        notifyNewArticle(articleId, a.getTitle());
    }

    private void sendPayload(String title, String body, String url, Long articleId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("body", body != null ? body : "");
        payload.put("url", url != null ? url : "/");
        if (articleId != null) {
            payload.put("articleId", articleId);
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("[push] serialize payload failed: {}", e.toString());
            return;
        }
        List<PushSubscription> subs = pushSubscriptionService.listAll();
        if (subs.isEmpty()) {
            return;
        }
        for (PushSubscription row : subs) {
            try {
                Subscription sub = new Subscription(row.getEndpoint(),
                        new Subscription.Keys(row.getP256dh(), row.getAuth()));
                Notification notification = new Notification(sub, json);
                HttpResponse resp = pushService.send(notification);
                int code = resp.getStatusLine().getStatusCode();
                EntityUtils.consumeQuietly(resp.getEntity());
                if (code == 410 || code == 404 || code == 403) {
                    pushSubscriptionService.deleteByEndpoint(row.getEndpoint());
                    continue;
                }
                if (code >= 400) {
                    log.debug("[push] endpoint returned {} for {}", code, row.getEndpoint());
                }
            } catch (Exception e) {
                log.debug("[push] send failed {}: {}", row.getEndpoint(), e.toString());
            }
        }
    }
}
