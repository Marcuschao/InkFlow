package com.blog.auth.common.util;

import org.springframework.util.StringUtils;

public final class DiaryTitleHelper {

    private DiaryTitleHelper() {
    }

    public static String resolveTitle(String title, String content) {
        if (StringUtils.hasText(title)) {
            return truncateByCodePoints(title.trim(), 30);
        }
        String line = firstLine(content);
        String stripped = stripLeadingHashes(line);
        if (!StringUtils.hasText(stripped)) {
            return "未命名";
        }
        return truncateByCodePoints(stripped.trim(), 30);
    }

    private static String firstLine(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        int i = 0;
        while (i < content.length() && (content.charAt(i) == '\r' || content.charAt(i) == '\n')) {
            i++;
        }
        int end = content.length();
        for (int j = i; j < content.length(); j++) {
            char c = content.charAt(j);
            if (c == '\n' || c == '\r') {
                end = j;
                break;
            }
        }
        return content.substring(i, end);
    }

    private static String stripLeadingHashes(String line) {
        String s = line;
        while (s.startsWith("#")) {
            s = s.substring(1);
        }
        return s.stripLeading();
    }

    public static String truncateByCodePoints(String s, int maxCp) {
        if (!StringUtils.hasText(s)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (int cp : s.codePoints().toArray()) {
            if (n >= maxCp) {
                break;
            }
            sb.appendCodePoint(cp);
            n++;
        }
        return sb.toString();
    }
}
