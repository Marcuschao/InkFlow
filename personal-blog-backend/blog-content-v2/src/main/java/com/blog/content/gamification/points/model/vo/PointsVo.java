package com.blog.content.gamification.points.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointsVo {
    private int totalPoints;
    private List<PointsLogVo> recentLogs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PointsLogVo {
        private int pointsChange;
        private String reason;
        private LocalDateTime createTime;
    }
}
