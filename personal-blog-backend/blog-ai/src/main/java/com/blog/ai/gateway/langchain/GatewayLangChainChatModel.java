package com.blog.ai.gateway.langchain;

import com.blog.ai.gateway.AiTaskType;
import com.blog.ai.gateway.service.AIGatewayService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.List;

public class GatewayLangChainChatModel implements ChatModel {

    private final AIGatewayService aiGatewayService;

    public GatewayLangChainChatModel(AIGatewayService aiGatewayService) {
        this.aiGatewayService = aiGatewayService;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        List<ChatMessage> messages = request.messages();
        String system = "";
        StringBuilder user = new StringBuilder();
        for (ChatMessage msg : messages) {
            if (msg instanceof SystemMessage sm) {
                system = sm.text();
            } else if (msg instanceof UserMessage um) {
                if (user.length() > 0) {
                    user.append("\n");
                }
                user.append(um.singleText());
            } else if (msg instanceof AiMessage am) {
                if (user.length() > 0) {
                    user.append("\n");
                }
                user.append(am.text());
            }
        }
        String content = aiGatewayService.chatContent(AiTaskType.AGENT, system, user.toString());
        return ChatResponse.builder()
                .aiMessage(AiMessage.from(content))
                .build();
    }
}
