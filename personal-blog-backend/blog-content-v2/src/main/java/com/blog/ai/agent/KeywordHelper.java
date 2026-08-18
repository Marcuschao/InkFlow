package com.blog.ai.agent;

import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class KeywordHelper {

    private static final Pattern SPLIT = Pattern.compile(
            "[\\s\\p{Punct}，。！？、；：\"\"''（）【】《》·…～]+");
    private static final Set<String> STOP = Set.of(
            "的", "了", "是", "我", "有", "和", "就", "不", "人", "在", "都", "一", "上", "也", "很", "到", "说",
            "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这", "吗", "吧", "呢", "啊", "什么",
            "怎么", "为什么", "哪些", "哪里", "谁", "嘛",
            "the", "a", "an", "is", "are", "was", "were", "be", "to", "of", "in", "on", "for", "at", "it",
            "and", "or", "but", "what", "how", "why", "who", "which", "when", "where", "do", "does", "did");

    private KeywordHelper() {
    }

    public static List<String> fromText(String text) {
        //判空
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        //去除首尾空白后再处理
        String trimmed = text.trim();
        List<String> parts = Arrays.stream(SPLIT.split(trimmed))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .filter(s -> s.length() >= 2 || s.matches("\\d+"))
                .filter(s -> !STOP.contains(s.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toCollection(ArrayList::new));
        //保持原有顺序的去重，最多保留前5个关键词
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (seen.add(p) && out.size() < 5) {
                out.add(p);
            }
        }
        if (out.isEmpty() && StringUtils.hasText(trimmed)) {
            String fallback = trimmed.length() > 32 ? trimmed.substring(0, 32) : trimmed;
            return List.of(fallback);
        }
        return out;
    }
}
