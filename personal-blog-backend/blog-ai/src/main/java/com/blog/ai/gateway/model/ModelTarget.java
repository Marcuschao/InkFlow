package com.blog.ai.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelTarget {
    private String providerId;
    private String model;
    private long timeoutMs;
    private double inputPricePer1k;
    private double outputPricePer1k;

    public String key() {
        return providerId + ":" + model;
    }
}
