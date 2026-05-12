package com.blog.personalblogbackend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(BlogFreshnessProperties.class)
public class BlogPhase5Config {

    @Bean(name = "translationBatchExecutor", destroyMethod = "shutdown")
    public ExecutorService translationBatchExecutor() {
        return Executors.newFixedThreadPool(2);
    }
}
