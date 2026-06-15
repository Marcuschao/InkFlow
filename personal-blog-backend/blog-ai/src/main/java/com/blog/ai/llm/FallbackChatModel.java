package com.blog.ai.llm;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

public class FallbackChatModel implements ChatModel {

    @Override
    public ChatResponse call(Prompt prompt) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(""))));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.just(call(prompt));
    }
}
