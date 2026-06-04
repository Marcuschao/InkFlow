package com.blog.personalblogbackend.cache;

import com.blog.personalblogbackend.monitor.BlogMetricsConfiguration;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class CacheMetricsRecorder {
    private final AtomicLong caffeineHit = new AtomicLong();
    private final AtomicLong caffeineMiss = new AtomicLong();
    private final AtomicLong redisHit = new AtomicLong();
    private final AtomicLong redisMiss = new AtomicLong();
    private final BlogMetricsConfiguration.BlogMeters meters;

    public CacheMetricsRecorder(BlogMetricsConfiguration.BlogMeters meters) {
        this.meters = meters;
    }

    public void recordCaffeineHit() {
        caffeineHit.incrementAndGet();
        meters.caffeineHit().increment();
    }

    public void recordCaffeineMiss() {
        caffeineMiss.incrementAndGet();
        meters.caffeineMiss().increment();
    }

    public void recordRedisHit() {
        redisHit.incrementAndGet();
        meters.redisHit().increment();
    }

    public void recordRedisMiss() {
        redisMiss.incrementAndGet();
        meters.redisMiss().increment();
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
