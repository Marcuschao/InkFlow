package com.blog.content.controller;

import com.blog.content.common.support.Result;
import com.blog.content.common.constant.BlogSiteKeys;
import com.blog.content.model.dto.site.PublicSiteConfigDto;
import com.blog.content.service.BlogSiteService;
import com.blog.content.service.SiteKvService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/site")
public class SitePublicController {

    @Autowired
    private SiteKvService siteKvService;
    @Autowired
    private BlogSiteService blogSiteService;

    @GetMapping("/public-config")
    public Result<PublicSiteConfigDto> publicConfig() {
        String mode = siteKvService.get(BlogSiteKeys.CHATBOT_VISIBILITY).orElse("NONE");
        return Result.success(new PublicSiteConfigDto(
                mode,
                blogSiteService.getSiteTitle(),
                blogSiteService.getSiteDescription(),
                blogSiteService.getSiteUrl(),
                blogSiteService.getLaunchTime()));
    }
}
