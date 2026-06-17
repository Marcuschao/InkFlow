package com.blog.auth.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "blog")
public class BlogSiteProperties {
    private String siteUrl = "";
    /** 前端部署子路径，如 /pblog；根路径部署留空 */
    private String siteBasePath = "";
    private String frontendBaseUrl = "http://tdwqlc.top";
    private boolean notifyMailEnabled = false;
    private String notifyFrom = "";
    private PushSettings push = new PushSettings();

    @Data
    public static class PushSettings {
        private boolean enabled = false;
        private String publicKey = "";
        private String privateKey = "";
        private String subject = "mailto:admin@localhost";
    }
}
