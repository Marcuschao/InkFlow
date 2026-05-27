package com.blog.personalblogbackend.cache;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class CacheMetricsRecorder {
    private final AtomicLong caffeineHit = new AtomicLong();
    private final AtomicLong caffeineMiss = new AtomicLong();
    private final AtomicLong redisHit = new AtomicLong();
    private final AtomicLong redisMiss = new AtomicLong();

    public void recordCaffeineHit() {
        caffeineHit.incrementAndGet();
    }

    public void recordCaffeineMiss() {
        caffeineMiss.incrementAndGet();
    }

    public void recordRedisHit() {
        redisHit.incrementAndGet();
    }

    public void recordRedisMiss() {
        redisMiss.incrementAndGet();
    }

    public CacheMetricsSnapshot snapshot() {
        long ch = caffeineHit.get();
        long cm = caffeineMiss.get();
        long rh = redisHit.get();
        long rm = redisMiss.get();
        return new CacheMetricsSnapshot(ch, cm, rh, rm,
                rate(ch, ch + cm), rate(rh, rh + rm));
    }

    private static double rate(long hit, long total) {
        if (total <= 0) {
            return 0D;
        }
        return hit * 100D / total;
    }

    public record CacheMetricsSnapshot(
            long caffeineHit, long caffeineMiss,
            long redisHit, long redisMiss,
            double caffeineHitRate, double redisHitRate) {
    }
}
