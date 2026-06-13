package com.blog.content.config.web;

import org.apache.catalina.core.StandardContext;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatShutdownConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatShutdownCustomizer() {
        return factory -> factory.addContextCustomizers(context -> {
            if (context instanceof StandardContext standardContext) {
                standardContext.setRenewThreadsWhenStoppingContext(true);
            }
        });
    }
}
