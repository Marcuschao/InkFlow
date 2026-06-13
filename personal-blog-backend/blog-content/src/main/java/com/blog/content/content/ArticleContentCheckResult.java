package com.blog.content.content;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ArticleContentCheckResult {
    private boolean passed = true;
    private int score = 100;
    private List<String> issues = new ArrayList<>();

    public void addIssue(String issue, int scorePenalty) {
        issues.add(issue);
        score = Math.max(0, score - scorePenalty);
        if (score < 60 || issues.size() >= 2) {
            passed = false;
        }
    }

    public String summary() {
        if (issues.isEmpty()) {
            return null;
        }
        return String.join("；", issues);
    }
}
