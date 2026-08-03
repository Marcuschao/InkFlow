package com.blog.ai.eval;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EvalMetrics {
    private static final Pattern CITATION = Pattern.compile("\\[(\\d+)]");
    private EvalMetrics() {}

    public static double reciprocalRank(List<Long> expected, List<Long> actual) {
        if (expected == null || expected.isEmpty()) return 1;
        for (int i = 0; i < actual.size(); i++) if (expected.contains(actual.get(i))) return 1.0 / (i + 1);
        return 0;
    }

    public static boolean citationsValid(String answer, int sourceCount) {
        Matcher matcher = CITATION.matcher(answer == null ? "" : answer);
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            if (index < 1 || index > sourceCount) return false;
        }
        return true;
    }

    public static boolean hasCitation(String answer) {
        return CITATION.matcher(answer == null ? "" : answer).find();
    }

    public static boolean isRefusal(String answer) {
        String value = answer == null ? "" : answer;
        return value.contains("无法") || value.contains("没有相关") || value.contains("抱歉") || value.contains("未找到");
    }
}
