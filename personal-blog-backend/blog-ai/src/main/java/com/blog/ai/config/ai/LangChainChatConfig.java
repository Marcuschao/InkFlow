package com.blog.ai.config.ai;

import com.blog.ai.agent.langchain.BlogChatAssistant;
import com.blog.ai.agent.tools.ArticleSearchTools;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class LangChainChatConfig {

    @Bean
    public ChatModel langChainChatModel(
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model:}") String model,
            @Value("${spring.ai.openai.base-url:}") String baseUrl) {
        OpenAiChatModel.OpenAiChatModelBuilder b = OpenAiChatModel.builder()
                .apiKey(apiKey == null ? "" : apiKey)
                .modelName(StringUtils.hasText(model) ? model : "gpt-4o-mini");
        if (StringUtils.hasText(baseUrl)) {
            String u = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            b.baseUrl(u);
        }
        return b.build();
    }

    @Bean
    public BlogChatAssistant blogChatAssistant(ChatModel langChainChatModel, ArticleSearchTools articleSearchTools) {
        return AiServices.builder(BlogChatAssistant.class)
                .chatModel(langChainChatModel)
                .tools(articleSearchTools)
                .build();
    }
}
