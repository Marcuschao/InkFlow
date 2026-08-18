package com.blog.ai.gateway.context;

public final class GatewayContext {

    private static final ThreadLocal<String> MODEL_USED = new ThreadLocal<>();

    private GatewayContext() {
    }

    public static void setModelUsed(String modelUsed) {
        MODEL_USED.set(modelUsed);
    }

    public static String getModelUsed() {
        return MODEL_USED.get();
    }

    public static void clear() {
        MODEL_USED.remove();
    }
}
