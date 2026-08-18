package com.blog.content.gamification.sign.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignResultVo {
    private boolean signedToday;
    private boolean alreadySigned;
    private int streakDays;
    private int totalDays;
    private int pointsEarned;
}
