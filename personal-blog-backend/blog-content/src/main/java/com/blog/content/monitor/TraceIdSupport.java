package com.blog.content.monitor;

import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.util.StringUtils;

import java.util.UUID;

public final class TraceIdSupport {

    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    private TraceIdSupport() {
    }

    public static String resolveOrCreate(String incoming) {
        if (StringUtils.hasText(incoming)) {
            return incoming.trim();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static MessagePostProcessor publishPostProcessor() {
        return message -> {
            String traceId = MDC.get(MDC_KEY);
            if (StringUtils.hasText(traceId)) {
                message.getMessageProperties().setHeader(HEADER, traceId);
            }
            return message;
        };
    }

    public static MessagePostProcessor receivePostProcessor() {
        return message -> {
            Object header = message.getMessageProperties().getHeader(HEADER);
            if (header != null) {
                MDC.put(MDC_KEY, header.toString());
            }
            return message;
        };
    }

    public static void clearMdc() {
        MDC.remove(MDC_KEY);
    }
}
