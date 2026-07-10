package com.blog.ai.config.ai;

import com.blog.ai.agent.langchain.BlogChatAssistant;
import com.blog.ai.agent.tools.ArticleSearchTools;
import com.blog.ai.gateway.langchain.GatewayLangChainChatModel;
import com.blog.ai.gateway.service.AIGatewayService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@Conditional(OnLlmApiKeyPresentCondition.class)
public class LangChainChatConfig {

    @Bean
    public ChatModel langChainChatModel(AIGatewayService aiGatewayService) {
        return new GatewayLangChainChatModel(aiGatewayService);
    }

    @Bean
    public BlogChatAssistant blogChatAssistant(ChatModel langChainChatModel, ArticleSearchTools articleSearchTools) {
        return AiServices.builder(BlogChatAssistant.class)
                .chatModel(langChainChatModel)
                .tools(articleSearchTools)
                .build();
    }
}
