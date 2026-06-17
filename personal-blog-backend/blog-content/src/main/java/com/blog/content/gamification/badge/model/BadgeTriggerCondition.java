package com.blog.content.gamification.badge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BadgeTriggerCondition {
    private String type;
    private int threshold;
}
