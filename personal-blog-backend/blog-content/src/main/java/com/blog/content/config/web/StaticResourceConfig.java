package com.blog.content.config.web;

import com.blog.content.config.properties.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Autowired
    private AppProperties appProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Path.of(appProperties.getUploadDir()).toAbsolutePath().normalize();
        registry.addResourceHandler("/upload/**").addResourceLocations(root.toUri().toString());
    }
}
