package com.blog.ai.gateway.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class AiCallLogProducer {

    public static final String EXCHANGE = "blog.ai.log";
    public static final String ROUTING_KEY = "ai.call.log";

    private final RabbitTemplate rabbitTemplate;

    public AiCallLogProducer(RabbitTemplate aiLogRabbitTemplate) {
        this.rabbitTemplate = aiLogRabbitTemplate;
    }

    public void send(AiCallLogEvent event) {
        rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
    }
}
