package com.blog.ai.rag.util;

public final class OpenAiCompatibleUrls {

    private OpenAiCompatibleUrls() {
    }

    public static String resolve(String baseUrl, String resourcePath) {
        String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        String base = trimSlash(baseUrl);
        if (base.endsWith("/v1")) {
            return base + path;
        }
        return base + "/v1" + path;
    }

    private static String trimSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String s = url.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
