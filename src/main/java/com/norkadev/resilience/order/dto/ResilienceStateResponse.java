package com.norkadev.resilience.order.dto;

public record ResilienceStateResponse(
        String name,
        String state,
        int bufferedCalls,
        int failedCalls,
        float failureRate,
        float slowCallRate) {
}
