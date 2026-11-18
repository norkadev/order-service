package com.norkadev.resilience.order.service;

import com.norkadev.resilience.order.dto.ResilienceStateResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.stereotype.Service;

@Service
public class ResilienceStateService {

    private final CircuitBreaker circuitBreaker;

    public ResilienceStateService(CircuitBreakerRegistry registry) {
        this.circuitBreaker = registry.circuitBreaker("inventory");
    }

    public ResilienceStateResponse currentState() {
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        return new ResilienceStateResponse(
                circuitBreaker.getName(),
                circuitBreaker.getState().name(),
                metrics.getNumberOfBufferedCalls(),
                metrics.getNumberOfFailedCalls(),
                metrics.getFailureRate(),
                metrics.getSlowCallRate());
    }
}
