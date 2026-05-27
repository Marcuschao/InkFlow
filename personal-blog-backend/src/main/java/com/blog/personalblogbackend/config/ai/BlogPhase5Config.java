package com.blog.personalblogbackend.config.ai;

import com.blog.personalblogbackend.config.properties.BlogFreshnessProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableConfigurationProperties(BlogFreshnessProperties.class)
public class BlogPhase5Config {

    @Bean(name = "translationBatchExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor translationBatchExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(2);
        ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("translation-");
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(30);
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.initialize();
        return ex;
    }
}
