package com.blog.auth.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private boolean enabled = true;
    private String endpoint = "http://127.0.0.1:9000";
    private String accessKey = "minioadmin";
    private String secretKey = "minioadmin";
    private String publicBaseUrl = "http://127.0.0.1:9000";
    private Buckets buckets = new Buckets();

    public String getBucket() {
        return buckets.getChat();
    }

    @Data
    public static class Buckets {
        private String chat = "blog-chat";
        private String avatars = "blog-avatars";
        private String diary = "blog-diary";
        private String versions = "blog-versions";
        private String logs = "blog-logs";
        private String reports = "blog-reports";
        private String backups = "blog-backups";
    }
}
