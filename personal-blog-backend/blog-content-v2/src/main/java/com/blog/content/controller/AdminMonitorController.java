package com.blog.content.controller;

import com.blog.content.cache.CacheMetricsRecorder;
import com.blog.content.common.support.Result;
import com.blog.content.monitor.SlowApiMonitorAspect;
import com.blog.content.chat.ChatMonitorService;
import com.blog.content.ratelimit.RateLimitMetricsRecorder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/monitor")
public class AdminMonitorController {
    private final SlowApiMonitorAspect slowApiMonitorAspect;
    private final CacheMetricsRecorder cacheMetricsRecorder;
    private final RateLimitMetricsRecorder rateLimitMetricsRecorder;
    private final ChatMonitorService chatMonitorService;

    public AdminMonitorController(SlowApiMonitorAspect slowApiMonitorAspect,
                                  CacheMetricsRecorder cacheMetricsRecorder,
                                  RateLimitMetricsRecorder rateLimitMetricsRecorder,
                                  ChatMonitorService chatMonitorService) {
        this.slowApiMonitorAspect = slowApiMonitorAspect;
        this.cacheMetricsRecorder = cacheMetricsRecorder;
        this.rateLimitMetricsRecorder = rateLimitMetricsRecorder;
        this.chatMonitorService = chatMonitorService;
    }

    @GetMapping("/slow")
    public Result<List<SlowApiMonitorAspect.SlowApiRecord>> slow() {
        long since = System.currentTimeMillis() - 3600_000L;
        return Result.success(slowApiMonitorAspect.topSlow(since, 10));
    }

    @GetMapping("/rate-limit")
    public Result<Map<String, Object>> rateLimit() {
        RateLimitMetricsRecorder.RateLimitMetricsSnapshot s = rateLimitMetricsRecorder.snapshot();
        Map<String, Object> data = new HashMap<>();
        data.put("rejectedTotal", s.rejectedTotal());
        data.put("allowedTotal", s.allowedTotal());
        return Result.success(data);
    }

    @GetMapping("/chat")
    public Result<Map<String, Object>> chat() {
        return Result.success(chatMonitorService.snapshot());
    }

    @GetMapping("/cache")
    public Result<Map<String, Object>> cache() {
        CacheMetricsRecorder.CacheMetricsSnapshot s = cacheMetricsRecorder.snapshot();
        Map<String, Object> data = new HashMap<>();
        data.put("caffeineHit", s.caffeineHit());
        data.put("caffeineMiss", s.caffeineMiss());
        data.put("caffeineHitRate", s.caffeineHitRate());
        data.put("redisHit", s.redisHit());
        data.put("redisMiss", s.redisMiss());
        data.put("redisHitRate", s.redisHitRate());
        return Result.success(data);
    }

    @GetMapping("/jvm")
    public Result<Map<String, Object>> jvm() {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        long gcCount = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
        Map<String, Object> data = new HashMap<>();
        data.put("heapUsed", memory.getHeapMemoryUsage().getUsed());
        data.put("heapMax", memory.getHeapMemoryUsage().getMax());
        data.put("threadCount", threads.getThreadCount());
        data.put("gcCount", gcCount);
        return Result.success(data);
    }

    @GetMapping("/system")
    public Result<Map<String, Object>> system() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        Map<String, Object> data = new HashMap<>();
        data.put("availableProcessors", os.getAvailableProcessors());
        data.put("systemLoadAverage", os.getSystemLoadAverage());
        return Result.success(data);
    }
}
