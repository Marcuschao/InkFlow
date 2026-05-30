package com.blog.personalblogbackend.ratelimit;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitMetricsRecorder {
    private final AtomicLong rejectedTotal = new AtomicLong();
    private final AtomicLong allowedTotal = new AtomicLong();

    public void recordRejected() {
        rejectedTotal.incrementAndGet();
    }

    public void recordAllowed() {
        allowedTotal.incrementAndGet();
    }

    public RateLimitMetricsSnapshot snapshot() {
        return new RateLimitMetricsSnapshot(rejectedTotal.get(), allowedTotal.get());
    }

    public record RateLimitMetricsSnapshot(long rejectedTotal, long allowedTotal) {
    }
}
