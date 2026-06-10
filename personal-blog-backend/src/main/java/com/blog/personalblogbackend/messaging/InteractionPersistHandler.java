package com.blog.personalblogbackend.messaging;

import com.blog.personalblogbackend.messaging.interaction.InteractionActionHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InteractionPersistHandler {

    private final Map<String, InteractionActionHandler> handlers;

    public InteractionPersistHandler(List<InteractionActionHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(InteractionActionHandler::action, Function.identity()));
    }

    @Transactional
    public void handle(InteractionPersistMessage message) {
        if (message == null || message.getAction() == null) {
            return;
        }
        InteractionActionHandler handler = handlers.get(message.getAction());
        if (handler != null) {
            handler.handle(message);
        }
    }
}
