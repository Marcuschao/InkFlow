package com.blog.personalblogbackend.config.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "statExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor statExecutor() {
        return buildExecutor("stat-", 2, 4, 500);
    }

    @Bean(name = "streamExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor streamExecutor() {
        return buildExecutor("stream-", 1, 2, 100);
    }

    private static ThreadPoolTaskExecutor buildExecutor(String prefix, int core, int max, int queue) {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(core);
        ex.setMaxPoolSize(max);
        ex.setQueueCapacity(queue);
        ex.setThreadNamePrefix(prefix);
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(30);
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.initialize();
        return ex;
    }
}
