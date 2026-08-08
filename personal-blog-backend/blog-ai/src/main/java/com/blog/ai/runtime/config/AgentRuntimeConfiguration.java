package com.blog.ai.runtime.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableConfigurationProperties(AgentRuntimeProperties.class)
public class AgentRuntimeConfiguration {
    @Bean(name = "agentRuntimeExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor runtimeExecutor(AgentRuntimeProperties p) {
        return executor("agent-runtime-", p.getRuntimeCorePoolSize(), p.getRuntimeMaxPoolSize(), p.getQueueCapacity());
    }

    @Bean(name = "agentToolExecutor", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor toolExecutor(AgentRuntimeProperties p) {
        return executor("agent-tool-", p.getToolCorePoolSize(), p.getToolMaxPoolSize(), p.getQueueCapacity());
    }

    @Bean(name = "agentEventPublisher", destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor eventExecutor(AgentRuntimeProperties p) {
        return executor("agent-event-", p.getEventCorePoolSize(), p.getEventMaxPoolSize(), p.getQueueCapacity());
    }

    private ThreadPoolTaskExecutor executor(String prefix, int core, int max, int queue) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setThreadNamePrefix(prefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
