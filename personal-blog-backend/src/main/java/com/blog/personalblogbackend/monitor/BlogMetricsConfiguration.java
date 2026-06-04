package com.blog.personalblogbackend.monitor;

import com.blog.personalblogbackend.chat.ChatMonitorService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BlogMetricsConfiguration {

    @Bean
    public BlogMeters blogMeters(MeterRegistry registry, ChatMonitorService chatMonitorService) {
        Counter caffeineHit = Counter.builder("blog.cache.caffeine.hit").register(registry);
        Counter caffeineMiss = Counter.builder("blog.cache.caffeine.miss").register(registry);
        Counter redisHit = Counter.builder("blog.cache.redis.hit").register(registry);
        Counter redisMiss = Counter.builder("blog.cache.redis.miss").register(registry);
        Counter rateLimitAllowed = Counter.builder("blog.ratelimit.allowed").register(registry);
        Counter rateLimitRejected = Counter.builder("blog.ratelimit.rejected").register(registry);
        Counter httpSlow = Counter.builder("blog.http.slow").register(registry);
        Timer httpServer = Timer.builder("blog.http.server.requests")
                .publishPercentileHistogram()
                .register(registry);

        Gauge.builder("blog.chat.online_users", chatMonitorService, s -> toDouble(s.snapshot().get("onlineUserCount")))
                .register(registry);
        Gauge.builder("blog.chat.offline_queue_length", chatMonitorService, s -> toDouble(s.snapshot().get("offlineQueueLength")))
                .register(registry);
        Gauge.builder("blog.chat.failed_queue_pending", chatMonitorService, s -> toDouble(s.snapshot().get("failedQueuePending")))
                .register(registry);
        Gauge.builder("blog.chat.stream_lag", chatMonitorService, s -> toDouble(s.snapshot().get("streamLag")))
                .register(registry);

        return new BlogMeters(caffeineHit, caffeineMiss, redisHit, redisMiss,
                rateLimitAllowed, rateLimitRejected, httpSlow, httpServer);
    }

    private static double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return 0D;
    }

    public record BlogMeters(
            Counter caffeineHit,
            Counter caffeineMiss,
            Counter redisHit,
            Counter redisMiss,
            Counter rateLimitAllowed,
            Counter rateLimitRejected,
            Counter httpSlow,
            Timer httpServer) {
    }
}
