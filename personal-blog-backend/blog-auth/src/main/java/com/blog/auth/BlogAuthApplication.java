package com.blog.auth;

import com.blog.common.config.CommonAutoConfiguration;
import jakarta.annotation.PostConstruct;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;

import java.util.TimeZone;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.blog.auth.mapper")
@Import(CommonAutoConfiguration.class)
public class BlogAuthApplication {

    @PostConstruct
    void initTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    }

    public static void main(String[] args) {
        SpringApplication.run(BlogAuthApplication.class, args);
    }
}
