package com.blog.personalblogbackend.config.rabbit;

import com.blog.personalblogbackend.monitor.TraceIdSupport;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTraceConfiguration {

    @Bean
    public BeanPostProcessor rabbitTraceBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof RabbitTemplate template) {
                    template.addBeforePublishPostProcessors(TraceIdSupport.publishPostProcessor());
                }
                if (bean instanceof SimpleRabbitListenerContainerFactory factory) {
                    factory.setAfterReceivePostProcessors(TraceIdSupport.receivePostProcessor());
                }
                return bean;
            }
        };
    }
}
