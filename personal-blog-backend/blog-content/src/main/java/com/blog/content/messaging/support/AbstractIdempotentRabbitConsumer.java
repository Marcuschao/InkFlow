package com.blog.content.messaging.support;

import com.blog.content.messaging.MqIdempotencyService;
import com.rabbitmq.client.Channel;

import java.io.IOException;

// 设计模式：模板方法 - 固定 Rabbit 消费 ACK/幂等流程，子类仅实现 doProcess
public abstract class AbstractIdempotentRabbitConsumer<M> {

    private final MqIdempotencyService idempotencyService;

    protected AbstractIdempotentRabbitConsumer(MqIdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    protected void consume(M message, Channel channel, long tag) throws IOException {
        String messageId = extractMessageId(message);
        if (message == null || messageId == null) {
            channel.basicAck(tag, false);
            return;
        }
        if (!idempotencyService.markIfAbsent(idempotencyQueue(), messageId)) {
            channel.basicAck(tag, false);
            return;
        }
        try {
            doProcess(message);
            channel.basicAck(tag, false);
        } catch (Exception ex) {
            logFailure(ex, message);
            channel.basicNack(tag, false, requeueOnFailure());
        }
    }

    protected abstract String idempotencyQueue();

    protected abstract String extractMessageId(M message);

    protected abstract void doProcess(M message) throws Exception;

    protected abstract void logFailure(Exception ex, M message);

    protected boolean requeueOnFailure() {
        return false;
    }
}
