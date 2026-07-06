package com.blog.ai.rag.messaging;

import com.blog.ai.config.properties.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "blog.rag.enabled", havingValue = "true")  //条件注解
public class RagMessageProducer {

    private final RabbitTemplate ragRabbitTemplate;
    private final RagProperties ragProperties;

    /**
     * 向解析队列投递文档 ID；成功返回 true，失败记录日志并返回 false（由调用方更新文档状态）。
     */
    public boolean sendParseTask(Long docId) {
        try {
            ragRabbitTemplate.convertAndSend(
                    ragProperties.getRabbit().getExchange(),
                    ragProperties.getRabbit().getRoutingKey(),
                    docId);

            return true;
        } catch (Exception e) {
            log.error("Failed to send parse task for document ID {}: {}", docId, e.getMessage(), e);
            return false;
        }
    }
}
