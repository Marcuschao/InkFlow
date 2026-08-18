package com.blog.content.concurrency;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class DistributedLockService {
    private final RedissonClient redissonClient;

    public DistributedLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public <T> T executeWithLock(String key, Supplier<T> action) {
        RLock lock = redissonClient.getLock(key);
        lock.lock();
        try {
            return action.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void executeWithLock(String key, Runnable action) {
        executeWithLock(key, () -> {
            action.run();
            return null;
        });
    }

    public boolean tryExecuteWithLock(String key, long waitSeconds, Runnable action) throws InterruptedException {
        RLock lock = redissonClient.getLock(key);
        if (lock.tryLock(waitSeconds, java.util.concurrent.TimeUnit.SECONDS)) {
            try {
                action.run();
                return true;
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
        return false;
    }
}
