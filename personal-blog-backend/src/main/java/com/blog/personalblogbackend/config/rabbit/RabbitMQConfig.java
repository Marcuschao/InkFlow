package com.blog.personalblogbackend.config.rabbit;

import com.blog.personalblogbackend.config.interaction.InteractionProperties;
import com.blog.personalblogbackend.config.properties.NotificationRabbitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
@ConditionalOnProperty(name = "blog.notification.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);
    public static final String DLX = "blog.dlx";

    @Value("${blog.notification.dead-letter-enabled:false}")
    private boolean notificationDeadLetterEnabled;

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public TopicExchange notificationExchange(NotificationRabbitProperties props) {
        return new TopicExchange(props.getExchange(), true, false);
    }

    @Bean
    public TopicExchange contentExchange(@Value("${blog.content.exchange:blog.content}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue inboxQueue(NotificationRabbitProperties props) {
        return notificationQueue(props.getInboxQueue());
    }

    @Bean
    public Queue pushQueue(NotificationRabbitProperties props) {
        return notificationQueue(props.getPushQueue());
    }

    @Bean
    public Queue mailQueue(NotificationRabbitProperties props) {
        return notificationQueue(props.getMailQueue());
    }

    @Bean
    public Queue auditQueue(NotificationRabbitProperties props) {
        return notificationQueue(props.getAuditQueue());
    }

    @Bean
    public Queue contentCacheQueue(@Value("${blog.content.queue:content.cache.queue}") String queue) {
        return queueWithDlq(queue);
    }

    @Bean
    public TopicExchange interactionExchange(InteractionProperties interactionProperties) {
        return new TopicExchange(interactionProperties.getExchange(), true, false);
    }

    @Bean
    public Queue interactionPersistQueue(InteractionProperties interactionProperties) {
        return QueueBuilder.durable(interactionProperties.getQueue()).build();
    }

    @Bean
    public Binding bindInteractionPersist(Queue interactionPersistQueue,
                                          TopicExchange interactionExchange,
                                          InteractionProperties interactionProperties) {
        return BindingBuilder.bind(interactionPersistQueue)
                .to(interactionExchange)
                .with(interactionProperties.getRoutingKey());
    }

    @Bean
    @ConditionalOnProperty(name = "blog.notification.dead-letter-enabled", havingValue = "true")
    public Queue inboxDlq(NotificationRabbitProperties props) {
        return QueueBuilder.durable(props.getInboxQueue() + ".dlq").build();
    }

    @Bean
    @ConditionalOnProperty(name = "blog.notification.dead-letter-enabled", havingValue = "true")
    public Queue pushDlq(NotificationRabbitProperties props) {
        return QueueBuilder.durable(props.getPushQueue() + ".dlq").build();
    }

    @Bean
    @ConditionalOnProperty(name = "blog.notification.dead-letter-enabled", havingValue = "true")
    public Queue mailDlq(NotificationRabbitProperties props) {
        return QueueBuilder.durable(props.getMailQueue() + ".dlq").build();
    }

    @Bean
    @ConditionalOnProperty(name = "blog.notification.dead-letter-enabled", havingValue = "true")
    public Queue auditDlq(NotificationRabbitProperties props) {
        return QueueBuilder.durable(props.getAuditQueue() + ".dlq").build();
    }

    @Bean
    public Queue contentCacheDlq(@Value("${blog.content.queue:content.cache.queue}") String queue) {
        return QueueBuilder.durable(queue + ".dlq").build();
    }

    @Bean
    public Binding bindLike(Queue inboxQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(inboxQueue).to(notificationExchange).with(NotificationRabbitProperties.RK_LIKE);
    }

    @Bean
    public Binding bindFavorite(Queue inboxQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(inboxQueue).to(notificationExchange).with(NotificationRabbitProperties.RK_FAVORITE);
    }

    @Bean
    public Binding bindComment(Queue inboxQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(inboxQueue).to(notificationExchange).with(NotificationRabbitProperties.RK_COMMENT);
    }

    @Bean
    public Binding bindFollow(Queue inboxQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(inboxQueue).to(notificationExchange).with(NotificationRabbitProperties.RK_FOLLOW);
    }

    @Bean
    public Binding bindPush(Queue pushQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(pushQueue).to(notificationExchange).with(NotificationRabbitProperties.RK_ARTICLE_PUBLISHED);
    }

    @Bean
    public Binding bindMail(Queue mailQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(mailQueue).to(notificationExchange).with(NotificationRabbitProperties.RK_ARTICLE_PUBLISHED);
    }

    @Bean
    public Binding bindAudit(Queue auditQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(auditQueue).to(notificationExchange).with("notification.event.#");
    }

    @Bean
    public Binding bindContentCache(Queue contentCacheQueue, TopicExchange contentExchange,
                                    @Value("${blog.content.routing-key:content.change}") String routingKey) {
        return BindingBuilder.bind(contentCacheQueue).to(contentExchange).with(routingKey);
    }

    @Bean
    @ConditionalOnProperty(name = "blog.notification.dead-letter-enabled", havingValue = "true")
    public Binding bindInboxDlq(Queue inboxDlq, DirectExchange deadLetterExchange, NotificationRabbitProperties props) {
        return BindingBuilder.bind(inboxDlq).to(deadLetterExchange).with(props.getInboxQueue() + ".dlq");
    }

    @Bean
    @ConditionalOnProperty(name = "blog.notification.dead-letter-enabled", havingValue = "true")
    public Binding bindPushDlq(Queue pushDlq, DirectExchange deadLetterExchange, NotificationRabbitProperties props) {
        return BindingBuilder.bind(pushDlq).to(deadLetterExchange).with(props.getPushQueue() + ".dlq");
    }

    @Bean
    @ConditionalOnProperty(name = "blog.notification.dead-letter-enabled", havingValue = "true")
    public Binding bindMailDlq(Queue mailDlq, DirectExchange deadLetterExchange, NotificationRabbitProperties props) {
        return BindingBuilder.bind(mailDlq).to(deadLetterExchange).with(props.getMailQueue() + ".dlq");
    }

    @Bean
    @ConditionalOnProperty(name = "blog.notification.dead-letter-enabled", havingValue = "true")
    public Binding bindAuditDlq(Queue auditDlq, DirectExchange deadLetterExchange, NotificationRabbitProperties props) {
        return BindingBuilder.bind(auditDlq).to(deadLetterExchange).with(props.getAuditQueue() + ".dlq");
    }

    @Bean
    public Binding bindContentCacheDlq(Queue contentCacheDlq, DirectExchange deadLetterExchange,
                                       @Value("${blog.content.queue:content.cache.queue}") String queue) {
        return BindingBuilder.bind(contentCacheDlq).to(deadLetterExchange).with(queue + ".dlq");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate.ConfirmCallback confirmCallback() {
        return (CorrelationData correlationData, boolean ack, String cause) -> {
            if (!ack) {
                log.warn("[rabbit] publish nack messageId={} cause={}",
                        correlationData != null ? correlationData.getId() : null, cause);
            }
        };
    }

    @Bean
    public RabbitTemplate.ReturnsCallback returnsCallback() {
        return returned -> log.warn("[rabbit] returned exchange={} routingKey={} reply={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText());
    }

    @Bean
    public RabbitTemplate rabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter,
                                         RabbitTemplate.ConfirmCallback confirmCallback,
                                         RabbitTemplate.ReturnsCallback returnsCallback) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setMandatory(true);
        template.setConfirmCallback(confirmCallback);
        template.setReturnsCallback(returnsCallback);
        return template;
    }

    private Queue notificationQueue(String queueName) {
        if (notificationDeadLetterEnabled) {
            return queueWithDlq(queueName);
        }
        return QueueBuilder.durable(queueName).build();
    }

    private static Queue queueWithDlq(String queueName) {
        return QueueBuilder.durable(queueName)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(queueName + ".dlq")
                .build();
    }
}
