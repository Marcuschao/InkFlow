package com.blog.personalblogbackend.notification.consumer;

import com.blog.personalblogbackend.notification.NotificationConsumeHelper;
import com.blog.personalblogbackend.notification.NotificationMessage;
import com.rabbitmq.client.Channel;

import java.io.IOException;

// 设计模式：模板方法 - 固定通知类 Rabbit 消费的幂等校验与 ACK 流程
public abstract class AbstractNotificationConsumer {

    private final NotificationConsumeHelper consumeHelper;

    protected AbstractNotificationConsumer(NotificationConsumeHelper consumeHelper) {
        this.consumeHelper = consumeHelper;
    }

    protected void consume(NotificationMessage message, Channel channel, long tag) throws IOException {
        NotificationConsumeHelper.ConsumeDecision decision = consumeHelper.prepare(queueKey(), message);
        if (decision != NotificationConsumeHelper.ConsumeDecision.PROCEED) {
            channel.basicAck(tag, false);
            return;
        }
        try {
            doProcess(message);
            channel.basicAck(tag, false);
        } catch (Exception ex) {
            logFailure(ex, message);
            channel.basicNack(tag, false, false);
        }
    }

    protected abstract String queueKey();

    protected abstract void doProcess(NotificationMessage message) throws Exception;

    protected abstract void logFailure(Exception ex, NotificationMessage message);
}
