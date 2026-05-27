package com.blog.personalblogbackend.controller;

import com.blog.personalblogbackend.cache.CacheMetricsRecorder;
import com.blog.personalblogbackend.common.support.Result;
import com.blog.personalblogbackend.monitor.SlowApiMonitorAspect;
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

    public AdminMonitorController(SlowApiMonitorAspect slowApiMonitorAspect,
                                  CacheMetricsRecorder cacheMetricsRecorder) {
        this.slowApiMonitorAspect = slowApiMonitorAspect;
        this.cacheMetricsRecorder = cacheMetricsRecorder;
    }

    @GetMapping("/slow")
    public Result<List<SlowApiMonitorAspect.SlowApiRecord>> slow() {
        long since = System.currentTimeMillis() - 3600_000L;
        return Result.success(slowApiMonitorAspect.topSlow(since, 10));
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
