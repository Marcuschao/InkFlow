package com.blog.content.config.concurrency;

import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class RedissonShutdownLifecycle implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(RedissonShutdownLifecycle.class);

    private final RedissonClient redissonClient;
    private volatile boolean running;

    public RedissonShutdownLifecycle(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        if (redissonClient != null && !redissonClient.isShutdown()) {
            log.info("Shutting down Redisson client");
            redissonClient.shutdown();
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }
}
