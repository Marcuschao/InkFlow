package com.blog.ai.llm;

import com.blog.ai.common.exception.ServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AiService {
    private static final String FALLBACK = "AI 服务暂时不可用，请稍后再试。";

    private final ChatModel chatModel;
    private final String apiKey;
    private final CircuitBreaker circuitBreaker;

    public AiService(ChatModel chatModel,
                     @Value("${spring.ai.openai.api-key:}") String apiKey,
                     CircuitBreakerRegistry circuitBreakerRegistry) {
        this.chatModel = chatModel;
        this.apiKey = apiKey;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("aiService");
    }

    public String chat(String systemPrompt, String userPrompt) {
        try {
            return circuitBreaker.executeSupplier(() -> doChat(systemPrompt, userPrompt));
        } catch (CallNotPermittedException ex) {
            return FALLBACK;
        } catch (Exception ex) {
            if (ex.getCause() instanceof ServiceException se) {
                throw se;
            }
            return FALLBACK;
        }
    }

    private String doChat(String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(apiKey)) {
            throw new ServiceException(503, "未配置 LLM API Key（spring.ai.openai.api-key）");
        }
        try {
            ChatResponse response = chatModel.call(new Prompt(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userPrompt)
            )));
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                throw new ServiceException(502, "LLM 返回空响应");
            }
            String content = response.getResult().getOutput().getText();
            if (!StringUtils.hasText(content)) {
                throw new ServiceException(502, "LLM 返回空响应");
            }
            return content.trim();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(502, "LLM 请求失败: " + e.getMessage());
        }
    }
}
