package com.blog.content.messaging.interaction;

import com.blog.content.messaging.InteractionPersistMessage;

// 设计模式：策略模式 - 按互动动作类型分发持久化逻辑，替代 if-else
public interface InteractionActionHandler {

    String action();

    void handle(InteractionPersistMessage message);
}
