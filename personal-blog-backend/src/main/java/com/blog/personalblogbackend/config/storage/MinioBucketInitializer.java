package com.blog.personalblogbackend.config.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

@Slf4j
public class MinioBucketInitializer implements ApplicationRunner {

    private static final int MAX_ATTEMPTS = 5;
    private static final long RETRY_MS = 2000L;

    private final MinioBucketSupport minioBucketSupport;

    public MinioBucketInitializer(MinioBucketSupport minioBucketSupport) {
        this.minioBucketSupport = minioBucketSupport;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                minioBucketSupport.ensureConfiguredBuckets();
                log.info("MinIO buckets initialized");
                return;
            } catch (Exception e) {
                if (attempt >= MAX_ATTEMPTS) {
                    log.error("MinIO bucket 启动初始化失败，已重试 {} 次", MAX_ATTEMPTS, e);
                    return;
                }
                log.warn("MinIO bucket 初始化第 {} 次失败，{}ms 后重试: {}", attempt, RETRY_MS, e.getMessage());
                sleep(RETRY_MS);
            }
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
