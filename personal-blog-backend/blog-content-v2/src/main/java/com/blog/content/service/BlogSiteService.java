package com.blog.content.service;

import java.time.LocalDateTime;

public interface BlogSiteService {
    String getSiteTitle();

    String getSiteDescription();

    String getSiteUrl();

    LocalDateTime getLaunchTime();

    String resolvePublicUrl(String path);
}
