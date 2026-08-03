package com.blog.ai.eval;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class EvalMetricsTest {
    @Test void calculatesReciprocalRank() {
        assertThat(EvalMetrics.reciprocalRank(List.of(8L), List.of(3L, 8L, 9L))).isEqualTo(0.5);
        assertThat(EvalMetrics.reciprocalRank(List.of(8L), List.of(3L, 9L))).isZero();
    }

    @Test void validatesCitationRange() {
        assertThat(EvalMetrics.citationsValid("依据[1]和[2]", 2)).isTrue();
        assertThat(EvalMetrics.citationsValid("不存在的引用[3]", 2)).isFalse();
        assertThat(EvalMetrics.hasCitation("没有引用")).isFalse();
    }

    @Test void detectsNoAnswerResponse() {
        assertThat(EvalMetrics.isRefusal("未找到相关资料，无法回答")).isTrue();
        assertThat(EvalMetrics.isRefusal("这是一个确定答案")).isFalse();
    }
}
