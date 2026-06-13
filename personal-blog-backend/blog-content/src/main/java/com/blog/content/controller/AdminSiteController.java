package com.blog.content.controller;

import com.blog.content.config.audit.Audit;
import com.blog.content.common.support.Result;
import com.blog.content.common.constant.BlogSiteKeys;
import com.blog.content.concurrency.DistributedLockService;
import com.blog.content.model.dto.site.ChatbotVisibilityBody;
import com.blog.content.model.dto.site.PublicSiteConfigDto;
import com.blog.content.service.BlogSiteService;
import com.blog.content.service.SiteKvService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/site")
public class AdminSiteController {

    @Autowired
    private SiteKvService siteKvService;
    @Autowired
    private BlogSiteService blogSiteService;
    @Autowired
    private DistributedLockService distributedLockService;

    @GetMapping("/public-config")
    public Result<PublicSiteConfigDto> getConfig() {
        String mode = siteKvService.get(BlogSiteKeys.CHATBOT_VISIBILITY).orElse("NONE");
        return Result.success(new PublicSiteConfigDto(
                mode,
                blogSiteService.getSiteTitle(),
                blogSiteService.getSiteDescription(),
                blogSiteService.getSiteUrl(),
                blogSiteService.getLaunchTime()));
    }

    @Audit("SITE_CHATBOT_VISIBILITY")
    @PutMapping("/chatbot-visibility")
    public Result<Void> setChatbotVisibility(@Valid @RequestBody ChatbotVisibilityBody body) {
        distributedLockService.executeWithLock("lock:site:settings", () ->
                siteKvService.put(BlogSiteKeys.CHATBOT_VISIBILITY, body.getMode()));
        return Result.success(null);
    }
}
