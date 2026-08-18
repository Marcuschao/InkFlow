package com.blog.ai.gateway.health;

import lombok.Data;

import java.time.Instant;

@Data
public class ProviderHealthState {
    public enum Status { HEALTHY, UNHEALTHY, HALF_OPEN }

    private Status status = Status.HEALTHY;
    private int consecutiveFailures;
    private Instant lastCheckAt;
    private Instant lastFailureAt;
    private String lastError;
}
