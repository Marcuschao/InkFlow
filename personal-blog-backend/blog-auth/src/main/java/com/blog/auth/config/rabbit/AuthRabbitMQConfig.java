package com.blog.auth.config.rabbit;

import com.blog.auth.config.properties.NotificationRabbitProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableRabbit
@ConditionalOnProperty(name = "blog.notification.enabled", havingValue = "true", matchIfMissing = true)
public class AuthRabbitMQConfig {

    @Bean
    public TopicExchange authNotificationExchange(NotificationRabbitProperties props) {
        return new TopicExchange(props.getExchange(), true, false);
    }

    @Bean
    public Queue passwordResetMailQueue(NotificationRabbitProperties props) {
        return QueueBuilder.durable(props.getPasswordResetMailQueue()).build();
    }

    @Bean
    public Binding bindPasswordResetRequestMail(Queue passwordResetMailQueue, TopicExchange authNotificationExchange) {
        return BindingBuilder.bind(passwordResetMailQueue)
                .to(authNotificationExchange)
                .with(NotificationRabbitProperties.RK_PASSWORD_RESET_REQUEST);
    }

    @Bean
    public Binding bindPasswordResetSuccessMail(Queue passwordResetMailQueue, TopicExchange authNotificationExchange) {
        return BindingBuilder.bind(passwordResetMailQueue)
                .to(authNotificationExchange)
                .with(NotificationRabbitProperties.RK_PASSWORD_RESET_SUCCESS);
    }

    @Bean
    public MessageConverter authMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    @Primary
    public RabbitTemplate authRabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory,
                                             MessageConverter authMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(authMessageConverter);
        return template;
    }
}
