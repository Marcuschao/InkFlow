package com.blog.content.config.storage;

import com.blog.content.config.properties.MinioProperties;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "minio", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @Bean
    public MinioBucketSupport minioBucketSupport(MinioClient minioClient, MinioProperties properties) {
        return new MinioBucketSupport(minioClient, properties);
    }

    @Bean
    public MinioBucketInitializer minioBucketInitializer(MinioBucketSupport minioBucketSupport) {
        return new MinioBucketInitializer(minioBucketSupport);
    }
}
