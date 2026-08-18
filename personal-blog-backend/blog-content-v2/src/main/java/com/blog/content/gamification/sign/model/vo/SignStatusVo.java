package com.blog.content.gamification.sign.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignStatusVo {
    private boolean signedToday;
    private int streakDays;
    private int totalDays;
    private int nextBonusDays;
    private int nextBonusPoints;
}
