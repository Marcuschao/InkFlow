package com.blog.personalblogbackend.monitor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class SlowApiMonitorAspect {
    private static final Logger log = LoggerFactory.getLogger(SlowApiMonitorAspect.class);
    private static final long WARN_MS = 500;
    private static final int MAX = 200;

    private final CopyOnWriteArrayList<SlowApiRecord> records = new CopyOnWriteArrayList<>();
    private final BlogMetricsConfiguration.BlogMeters meters;
    private final MeterRegistry meterRegistry;

    public SlowApiMonitorAspect(BlogMetricsConfiguration.BlogMeters meters, MeterRegistry meterRegistry) {
        this.meters = meters;
        this.meterRegistry = meterRegistry;
    }

    @Around("@within(org.springframework.web.bind.annotation.RestController) && execution(public * *(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String signature = pjp.getSignature().toShortString();
        try {
            return pjp.proceed();
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (cost >= WARN_MS) {
                log.warn("[slow-api] {} took {}ms", signature, cost);
                meters.httpSlow().increment();
            }
            Timer.builder("blog.http.server.requests")
                    .tag("endpoint", signature)
                    .register(meterRegistry)
                    .record(cost, TimeUnit.MILLISECONDS);
            meters.httpServer().record(cost, TimeUnit.MILLISECONDS);
            records.add(new SlowApiRecord(signature, cost, System.currentTimeMillis()));
            trim();
        }
    }

    public List<SlowApiRecord> topSlow(long sinceMillis, int limit) {
        return records.stream()
                .filter(r -> r.timestamp >= sinceMillis)
                .sorted(Comparator.comparingLong(SlowApiRecord::costMs).reversed())
                .limit(limit)
                .toList();
    }

    public List<SlowApiRecord> snapshotAll() {
        return List.copyOf(records);
    }

    private void trim() {
        if (records.size() <= MAX) {
            return;
        }
        long cutoff = System.currentTimeMillis() - 3600_000L;
        records.removeIf(r -> r.timestamp < cutoff);
    }

    public record SlowApiRecord(String endpoint, long costMs, long timestamp) {
    }
}
