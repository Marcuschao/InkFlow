package com.blog.ai.gateway.health;

import com.blog.ai.gateway.config.GatewayProperties;
import com.blog.ai.gateway.config.ModelProviderConfig;
import com.blog.ai.gateway.factory.ChatModelFactory;
import com.blog.ai.gateway.factory.GatewayChatModels;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModelHealthCheckerTest {

    @Test
    void probesProviderWithLangChainModel() {
        GatewayProperties properties = new GatewayProperties();
        properties.setHealthCheckEnabled(true);
        ModelProviderConfig providers = mock(ModelProviderConfig.class);
        ChatModelFactory factory = mock(ChatModelFactory.class);
        ChatModel chatModel = mock(ChatModel.class);
        GatewayProperties.ProviderDef provider = new GatewayProperties.ProviderDef();
        provider.setId("primary");
        provider.setApiKey("test-key");
        provider.setModels(List.of("test-model"));
        when(providers.listEnabledProviders()).thenReturn(List.of(provider));
        when(factory.get(any())).thenReturn(new GatewayChatModels(chatModel, mock(StreamingChatModel.class)));
        when(chatModel.chat(any(List.class))).thenReturn(
                ChatResponse.builder().aiMessage(AiMessage.from("pong")).build());
        ModelHealthChecker checker = new ModelHealthChecker(properties, providers, factory);

        checker.scheduledCheck();

        assertThat(checker.getState("primary").getStatus()).isEqualTo(ProviderHealthState.Status.HEALTHY);
        assertThat(checker.getState("primary").getConsecutiveFailures()).isZero();
    }
}
