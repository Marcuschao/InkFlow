package com.blog.ai.model.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class AiStatsTrendDto {
    private List<String> labels;
    private List<Long> calls;
    private List<Double> costs;
    private List<Double> successRates;
}
