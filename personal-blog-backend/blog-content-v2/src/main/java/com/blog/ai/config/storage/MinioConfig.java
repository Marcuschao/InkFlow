package com.blog.ai.config.storage;

import com.blog.ai.config.properties.MinioProperties;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "minio", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MinioConfig {

    @Bean("aiMinioBucketSupport")
    public MinioBucketSupport aiMinioBucketSupport(MinioClient minioClient, MinioProperties properties) {
        return new MinioBucketSupport(minioClient, properties);
    }

    @Bean("aiMinioBucketInitializer")
    public MinioBucketInitializer aiMinioBucketInitializer(MinioBucketSupport minioBucketSupport) {
        return new MinioBucketInitializer(minioBucketSupport);
    }
}
