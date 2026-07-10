package com.blog.ai.gateway.mq;

import com.blog.ai.mapper.AiCallLogMapper;
import com.blog.ai.model.entity.AiCallLog;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiCallLogConsumer {

    private final AiCallLogMapper aiCallLogMapper;

    public AiCallLogConsumer(AiCallLogMapper aiCallLogMapper) {
        this.aiCallLogMapper = aiCallLogMapper;
    }

    @RabbitListener(queues = AiGatewayRabbitConfig.QUEUE, containerFactory = "aiLogListenerContainerFactory")
    public void consume(AiCallLogEvent event) {
        AiCallLog row = new AiCallLog();
        row.setUserId(event.getUserId());
        row.setUsername(event.getUsername());
        row.setTaskType(event.getTaskType());
        row.setProvider(event.getProvider());
        row.setModel(event.getModel());
        row.setInputTokens(event.getInputTokens());
        row.setOutputTokens(event.getOutputTokens());
        row.setCost(event.getCost());
        row.setLatencyMs(event.getLatencyMs());
        row.setStatus(event.getStatus());
        row.setErrorMsg(event.getErrorMsg());
        row.setPromptHash(event.getPromptHash());
        row.setFeature(StringUtils.hasText(event.getFeature()) ? event.getFeature()
                : (StringUtils.hasText(event.getTaskType()) ? event.getTaskType() : "unknown"));
        row.setSuccess(event.getSuccess());
        row.setDurationMs(event.getLatencyMs());
        row.setCreatedAt(event.getCreatedAt());
        aiCallLogMapper.insert(row);
    }
}
