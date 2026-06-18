package com.blog.content;

import com.blog.common.config.CommonAutoConfiguration;
import jakarta.annotation.PostConstruct;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan({
        "com.blog.content.mapper",
        "com.blog.content.gamification.badge.mapper",
        "com.blog.content.gamification.points.mapper",
        "com.blog.content.gamification.reward.mapper",
        "com.blog.content.gamification.shop.mapper",
        "com.blog.content.profile.mapper"
})
@EnableScheduling
@EnableAsync
@Import(CommonAutoConfiguration.class)
public class BlogContentApplication {

    @PostConstruct
    void initTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    }

    public static void main(String[] args) {
        SpringApplication.run(BlogContentApplication.class, args);
    }
}
