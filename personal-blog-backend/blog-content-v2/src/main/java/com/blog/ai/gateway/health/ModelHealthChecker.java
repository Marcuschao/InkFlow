package com.blog.ai.gateway.health;

import com.blog.ai.gateway.config.GatewayProperties;
import com.blog.ai.gateway.config.ModelProviderConfig;
import com.blog.ai.gateway.factory.ChatModelFactory;
import com.blog.ai.gateway.factory.GatewayChatModels;
import com.blog.ai.gateway.model.ModelTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ModelHealthChecker {

    private static final Logger log = LoggerFactory.getLogger(ModelHealthChecker.class);

    private final GatewayProperties gatewayProperties;
    private final ModelProviderConfig modelProviderConfig;
    private final ChatModelFactory chatModelFactory;
    private final Map<String, ProviderHealthState> states = new ConcurrentHashMap<>();

    public ModelHealthChecker(GatewayProperties gatewayProperties,
                              ModelProviderConfig modelProviderConfig,
                              ChatModelFactory chatModelFactory) {
        this.gatewayProperties = gatewayProperties;
        this.modelProviderConfig = modelProviderConfig;
        this.chatModelFactory = chatModelFactory;
    }

    public boolean isAvailable(String providerId) {
        ProviderHealthState state = states.get(providerId);
        if (state == null) {
            return true;
        }
        return state.getStatus() != ProviderHealthState.Status.UNHEALTHY;
    }

    public Map<String, ProviderHealthState> snapshot() {
        return Map.copyOf(states);
    }

    public ProviderHealthState getState(String providerId) {
        return states.computeIfAbsent(providerId, k -> new ProviderHealthState());
    }

    @Scheduled(initialDelayString = "${blog.ai.gateway.health-check-initial-delay-ms:60000}",
            fixedDelayString = "${blog.ai.gateway.health-check-interval-ms:30000}")
    public void scheduledCheck() {
        if (!gatewayProperties.isHealthCheckEnabled()) {
            return;
        }
        for (GatewayProperties.ProviderDef def : modelProviderConfig.listEnabledProviders()) {
            if (!StringUtils.hasText(def.getApiKey())) {
                continue;
            }
            ProviderHealthState state = getState(def.getId());
            if (state.getStatus() == ProviderHealthState.Status.UNHEALTHY) {
                continue;
            }
            probe(def.getId(), def.getModels().get(0), state);
        }
    }

    @Scheduled(fixedDelayString = "${blog.ai.gateway.half-open-interval-ms:60000}")
    public void halfOpenProbe() {
        if (!gatewayProperties.isHealthCheckEnabled()) {
            return;
        }
        for (GatewayProperties.ProviderDef def : modelProviderConfig.listEnabledProviders()) {
            ProviderHealthState state = getState(def.getId());
            if (state.getStatus() != ProviderHealthState.Status.UNHEALTHY) {
                continue;
            }
            state.setStatus(ProviderHealthState.Status.HALF_OPEN);
            boolean ok = probe(def.getId(), def.getModels().get(0), state);
            if (ok) {
                state.setConsecutiveFailures(0);
                state.setStatus(ProviderHealthState.Status.HEALTHY);
                log.info("Provider {} 恢复健康", def.getId());
            } else {
                state.setStatus(ProviderHealthState.Status.UNHEALTHY);
            }
        }
    }

    public void recordSuccess(String providerId) {
        ProviderHealthState state = getState(providerId);
        state.setConsecutiveFailures(0);
        state.setStatus(ProviderHealthState.Status.HEALTHY);
        state.setLastCheckAt(Instant.now());
    }

    public void recordFailure(String providerId, String error) {
        ProviderHealthState state = getState(providerId);
        state.setConsecutiveFailures(state.getConsecutiveFailures() + 1);
        state.setLastFailureAt(Instant.now());
        state.setLastError(error);
        state.setLastCheckAt(Instant.now());
        if (state.getConsecutiveFailures() >= gatewayProperties.getFailureThreshold()) {
            state.setStatus(ProviderHealthState.Status.UNHEALTHY);
            log.warn("Provider {} 标记为 UNHEALTHY: {}", providerId, error);
        }
    }

    private boolean probe(String providerId, String model, ProviderHealthState state) {
        try {
            ModelTarget target = new ModelTarget(providerId, model, 5000, 0, 0);
            GatewayChatModels models = chatModelFactory.get(target);
            ChatModel chatModel = models.chatModel();
            ChatResponse response = chatModel.chat(List.of(UserMessage.from("ping")));
            boolean ok = response != null && response.aiMessage() != null
                    && StringUtils.hasText(response.aiMessage().text());
            state.setLastCheckAt(Instant.now());
            if (ok) {
                state.setConsecutiveFailures(0);
                state.setStatus(ProviderHealthState.Status.HEALTHY);
            } else {
                recordFailure(providerId, "empty response");
            }
            return ok;
        } catch (Exception e) {
            recordFailure(providerId, e.getMessage());
            return false;
        }
    }
}
