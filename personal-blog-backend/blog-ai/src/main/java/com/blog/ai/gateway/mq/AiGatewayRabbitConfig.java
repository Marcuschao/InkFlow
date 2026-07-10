package com.blog.ai.gateway.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableRabbit
public class AiGatewayRabbitConfig {

    public static final String EXCHANGE = AiCallLogProducer.EXCHANGE;
    public static final String QUEUE = "ai.call.log.queue";

    @Bean
    public DirectExchange aiLogExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue aiCallLogQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding aiCallLogBinding(Queue aiCallLogQueue, DirectExchange aiLogExchange) {
        return BindingBuilder.bind(aiCallLogQueue).to(aiLogExchange).with(AiCallLogProducer.ROUTING_KEY);
    }

    @Bean
    public MessageConverter aiLogMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory aiLogListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter aiLogMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(aiLogMessageConverter);
        return factory;
    }

    @Bean
    public RabbitTemplate aiLogRabbitTemplate(ConnectionFactory connectionFactory,
                                              MessageConverter aiLogMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(aiLogMessageConverter);
        return template;
    }
}
