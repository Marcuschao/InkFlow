package com.blog.ai.config.ai;

import com.blog.ai.llm.FallbackChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class AiChatModelConfiguration {

    @Bean
    @Conditional(OnLlmApiKeyPresentCondition.class)
    public ChatModel openAiChatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String model,
            @Value("${spring.ai.openai.chat.options.temperature:0.7}") Double temperature,
            @Value("${spring.ai.openai.chat.options.max-tokens:1024}") Integer maxTokens) {
        OpenAiApi.Builder apiBuilder = OpenAiApi.builder().apiKey(apiKey);
        if (StringUtils.hasText(baseUrl)) {
            String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            apiBuilder.baseUrl(normalized);
        }
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(apiBuilder.build())
                .defaultOptions(options)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel fallbackChatModel() {
        return new FallbackChatModel();
    }
}
