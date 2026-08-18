package com.blog.content.controller;

import com.blog.content.model.dto.push.AdminPushSendRequest;
import com.blog.content.model.dto.push.PushStatsResponse;
import com.blog.content.common.exception.ServiceException;
import com.blog.content.service.BlogSiteService;
import com.blog.content.service.WebPushService;
import com.blog.content.service.impl.PushSubscriptionServiceImpl;
import com.blog.content.common.support.Result;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/push")
public class PushAdminController {

    private final WebPushService webPushService;
    private final PushSubscriptionServiceImpl pushSubscriptionService;
    private final BlogSiteService blogSiteService;

    public PushAdminController(WebPushService webPushService,
                               PushSubscriptionServiceImpl pushSubscriptionService,
                               BlogSiteService blogSiteService) {
        this.webPushService = webPushService;
        this.pushSubscriptionService = pushSubscriptionService;
        this.blogSiteService = blogSiteService;
    }

    @GetMapping("/stats")
    public Result<PushStatsResponse> stats() {
        long n = pushSubscriptionService.countActive();
        return Result.success(new PushStatsResponse(n));
    }

    @PostMapping("/send")
    public Result<Void> send(@RequestBody AdminPushSendRequest body) {
        if (!webPushService.isOperational()) {
            throw new ServiceException(400, "推送未启用或密钥无效");
        }
        if (body != null && body.getArticleId() != null) {
            webPushService.sendForArticle(body.getArticleId());
            return Result.success(null);
        }
        if (body == null || !StringUtils.hasText(body.getTitle()) || !StringUtils.hasText(body.getBody())) {
            throw new ServiceException(400, "请提供 articleId 或 title+body");
        }
        String url = StringUtils.hasText(body.getUrl())
                ? body.getUrl()
                : blogSiteService.resolvePublicUrl("/");
        webPushService.sendCustom(body.getTitle(), body.getBody(), url);
        return Result.success(null);
    }
}
