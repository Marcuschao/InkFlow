package com.blog.ai.llm;

import com.blog.ai.gateway.AiTaskType;
import com.blog.ai.gateway.service.AIGatewayService;
import com.blog.ai.common.exception.ServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.stereotype.Service;

@Service
public class AiService {
    private static final String FALLBACK = "AI 服务暂时不可用，请稍后再试。";

    private final AIGatewayService aiGatewayService;
    private final CircuitBreaker circuitBreaker;

    public AiService(AIGatewayService aiGatewayService, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.aiGatewayService = aiGatewayService;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("aiService");
    }

    public String chat(String systemPrompt, String userPrompt) {
        return chat(AiTaskType.GENERIC, systemPrompt, userPrompt);
    }

    public String chat(AiTaskType taskType, String systemPrompt, String userPrompt) {
        try {
            return circuitBreaker.executeSupplier(() -> aiGatewayService.chatContent(taskType, systemPrompt, userPrompt));
        } catch (CallNotPermittedException ex) {
            return FALLBACK;
        } catch (ServiceException se) {
            throw se;
        } catch (Exception ex) {
            if (ex.getCause() instanceof ServiceException se) {
                throw se;
            }
            return FALLBACK;
        }
    }
}
