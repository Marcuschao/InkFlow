package com.blog.ai.config.rabbit;

import com.blog.ai.config.properties.RagProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
@ConditionalOnProperty(name = "blog.rag.enabled", havingValue = "true")
public class RagRabbitConfig {

    public static final String DLX = "blog.rag.dlx";

    @Bean
    public TopicExchange ragExchange(RagProperties props) {
        return new TopicExchange(props.getRabbit().getExchange(), true, false);
    }

    @Bean
    public DirectExchange ragDlx() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue ragDocParseQueue(RagProperties props) {
        if (props.getRabbit().isDeadLetterEnabled()) {
            return QueueBuilder.durable(props.getRabbit().getQueue())
                    .deadLetterExchange(DLX)
                    .deadLetterRoutingKey(props.getRabbit().getQueue() + ".dlq")
                    .build();
        }
        return QueueBuilder.durable(props.getRabbit().getQueue()).build();
    }

    @Bean
    public Binding bindRagDocParse(Queue ragDocParseQueue, TopicExchange ragExchange, RagProperties props) {
        return BindingBuilder.bind(ragDocParseQueue).to(ragExchange).with(props.getRabbit().getRoutingKey());
    }

    @Bean
    public RabbitAdmin ragRabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setIgnoreDeclarationExceptions(true);
        return admin;
    }

    @Bean
    public MessageConverter ragMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate ragRabbitTemplate(ConnectionFactory connectionFactory,
                                            @Qualifier("ragMessageConverter") MessageConverter ragMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(ragMessageConverter);
        template.setMandatory(true);
        return template;
    }
}
