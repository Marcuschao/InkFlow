package com.blog.auth.messaging;

import com.blog.auth.model.dto.mail.PasswordResetMailMessage;
import com.blog.auth.service.MailService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "blog.notification.enabled", havingValue = "true", matchIfMissing = true)
public class PasswordResetMailConsumer {

    private final MailService mailService;

    @RabbitListener(queues = "#{notificationRabbitProperties.passwordResetMailQueue}")
    public void onMessage(PasswordResetMailMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        if (message == null || message.getEmail() == null) {
            channel.basicAck(tag, false);
            return;
        }
        if ("RESET_REQUEST".equals(message.getType())) {
            mailService.sendPasswordResetLink(message.getEmail(), message.getUsername(), message.getResetLink());
            channel.basicAck(tag, false);
            return;
        }
        if ("RESET_SUCCESS".equals(message.getType())) {
            mailService.sendPasswordResetSuccess(message.getEmail(), message.getUsername());
            channel.basicAck(tag, false);
            return;
        }
        log.warn("unknown password reset mail type={}", message.getType());
        channel.basicAck(tag, false);
    }
}
