package com.blog.content.config.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.net.ConnectException;
import java.util.concurrent.CompletableFuture;

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
        CompletableFuture.runAsync(this::initializeBuckets);
    }

    private void initializeBuckets() {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                minioBucketSupport.ensureConfiguredBuckets();
                log.info("MinIO buckets initialized");
                return;
            } catch (Exception e) {
                if (!isRetryable(e) || attempt >= MAX_ATTEMPTS) {
                    log.error("MinIO bucket 启动初始化失败: {}", rootMessage(e), e);
                    return;
                }
                log.warn("MinIO bucket 初始化第 {} 次失败，{}ms 后重试: {}", attempt, RETRY_MS, rootMessage(e));
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

    private static boolean isRetryable(Exception e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof ConnectException) {
                return false;
            }
            t = t.getCause();
        }
        return true;
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }
}
