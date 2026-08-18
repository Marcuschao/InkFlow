package com.blog.content.v2;

import com.blog.common.config.CommonAutoConfiguration;
import jakarta.annotation.PostConstruct;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication(
        scanBasePackages = {"com.blog.content", "com.blog.ai"},
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EnableDiscoveryClient
@MapperScan(
        basePackages = {
                "com.blog.content.mapper",
                "com.blog.content.gamification.badge.mapper",
                "com.blog.content.gamification.points.mapper",
                "com.blog.content.gamification.reward.mapper",
                "com.blog.content.gamification.shop.mapper",
                "com.blog.content.profile.mapper",
                "com.blog.ai.mapper",
                "com.blog.ai.runtime.mapper"
        },
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EnableScheduling
@EnableAsync
@Import(CommonAutoConfiguration.class)
public class BlogContentV2Application {

    @PostConstruct
    void initTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    }

    public static void main(String[] args) {
        SpringApplication.run(BlogContentV2Application.class, args);
    }
}
