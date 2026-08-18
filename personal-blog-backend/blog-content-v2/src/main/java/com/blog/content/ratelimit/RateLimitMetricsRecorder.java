package com.blog.content.ratelimit;

import com.blog.content.monitor.BlogMetricsConfiguration;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitMetricsRecorder {
    private final AtomicLong rejectedTotal = new AtomicLong();
    private final AtomicLong allowedTotal = new AtomicLong();
    private final BlogMetricsConfiguration.BlogMeters meters;

    public RateLimitMetricsRecorder(BlogMetricsConfiguration.BlogMeters meters) {
        this.meters = meters;
    }

    public void recordRejected() {
        rejectedTotal.incrementAndGet();
        meters.rateLimitRejected().increment();
    }

    public void recordAllowed() {
        allowedTotal.incrementAndGet();
        meters.rateLimitAllowed().increment();
    }

    public RateLimitMetricsSnapshot snapshot() {
        return new RateLimitMetricsSnapshot(rejectedTotal.get(), allowedTotal.get());
    }

    public record RateLimitMetricsSnapshot(long rejectedTotal, long allowedTotal) {
    }
}
