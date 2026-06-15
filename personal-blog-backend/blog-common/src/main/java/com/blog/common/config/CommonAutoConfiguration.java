package com.blog.common.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.blog.common")
@EnableFeignClients(basePackages = "com.blog.common.feign")
public class CommonAutoConfiguration {
}
