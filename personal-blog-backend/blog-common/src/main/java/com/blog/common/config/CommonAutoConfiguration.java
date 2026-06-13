package com.blog.common.config;

import com.blog.common.exception.GlobalExceptionHandler;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(
        basePackages = "com.blog.common",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = GlobalExceptionHandler.class
        )
)
@EnableFeignClients(basePackages = "com.blog.common.feign")
public class CommonAutoConfiguration {
}
