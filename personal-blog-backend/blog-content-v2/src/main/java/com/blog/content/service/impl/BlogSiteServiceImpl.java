package com.blog.content.service.impl;

import com.blog.content.config.properties.BlogSiteProperties;
import com.blog.content.mapper.BlogSiteMapper;
import com.blog.content.model.entity.BlogSite;
import com.blog.content.service.BlogSiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BlogSiteServiceImpl implements BlogSiteService {

    private static final long SITE_ID = 1L;

    private final BlogSiteMapper blogSiteMapper;
    private final BlogSiteProperties blogSiteProperties;

    @Override
    public String getSiteTitle() {
        BlogSite site = loadSite();
        return site != null ? site.getSiteTitle() : "晓晓博客";
    }

    @Override
    public String getSiteDescription() {
        BlogSite site = loadSite();
        return site != null ? site.getSiteDescription() : "个人博客";
    }

    @Override
    public String getSiteUrl() {
        BlogSite site = loadSite();
        return site != null ? site.getSiteUrl() : "http://localhost:5173";
    }

    @Override
    public LocalDateTime getLaunchTime() {
        BlogSite site = loadSite();
        return site != null ? site.getLaunchTime() : LocalDateTime.of(2026, 5, 1, 0, 0);
    }

    @Override
    public String resolvePublicUrl(String path) {
        String origin = getSiteUrl().replaceAll("/$", "");
        String basePath = blogSiteProperties.getSiteBasePath() != null
                ? blogSiteProperties.getSiteBasePath().trim() : "";
        if (!basePath.isEmpty()) {
            if (!basePath.startsWith("/")) {
                basePath = "/" + basePath;
            }
            basePath = basePath.replaceAll("/$", "");
        }
        String p = path != null ? path : "/";
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return origin + basePath + p;
    }

    private BlogSite loadSite() {
        return blogSiteMapper.selectById(SITE_ID);
    }
}
