package com.blog.ai.gateway.factory;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

public record GatewayChatModels(ChatModel chatModel, StreamingChatModel streamingChatModel) {
}
