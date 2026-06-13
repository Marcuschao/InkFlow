package com.blog.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(exclude = RedisReactiveAutoConfiguration.class)
@EnableDiscoveryClient
public class BlogGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogGatewayApplication.class, args);
    }
}
