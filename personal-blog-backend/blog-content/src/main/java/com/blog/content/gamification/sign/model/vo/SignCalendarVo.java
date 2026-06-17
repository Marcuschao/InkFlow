package com.blog.content.gamification.sign.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignCalendarVo {
    private int year;
    private int month;
    private List<Boolean> days;
    private int signedCount;
}
