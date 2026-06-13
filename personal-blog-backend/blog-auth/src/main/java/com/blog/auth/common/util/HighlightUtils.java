package com.blog.auth.common.util;

import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.util.Arrays;
import java.util.regex.Pattern;

public final class HighlightUtils {

    private static final Pattern NON_WORD = Pattern.compile("\\s+");

    private HighlightUtils() {
    }

    public static String highlight(String raw, String keyword) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String escaped = HtmlUtils.htmlEscape(raw);
        if (!StringUtils.hasText(keyword)) {
            return escaped;
        }
        String[] parts = NON_WORD.split(keyword.trim());
        String result = escaped;
        for (String p : parts) {
            if (!StringUtils.hasText(p)) {
                continue;
            }
            String token = HtmlUtils.htmlEscape(p.trim());
            if (!StringUtils.hasText(token)) {
                continue;
            }
            result = highlightIgnoreCase(result, token);
        }
        return result;
    }

    private static String highlightIgnoreCase(String text, String token) {
        String lower = text.toLowerCase();
        String tlow = token.toLowerCase();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int idx = lower.indexOf(tlow, i);
            if (idx < 0) {
                sb.append(text, i, text.length());
                break;
            }
            sb.append(text, i, idx);
            sb.append("<mark>");
            sb.append(text, idx, idx + token.length());
            sb.append("</mark>");
            i = idx + token.length();
        }
        return sb.toString();
    }
}
