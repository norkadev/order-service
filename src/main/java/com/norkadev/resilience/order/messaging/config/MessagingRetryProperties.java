package com.norkadev.resilience.order.messaging.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "app.messaging.retry")
public record MessagingRetryProperties(
        @Min(1) int maxAttempts,
        @NotNull Duration initialDelay,
        @DecimalMin("1.0") double multiplier,
        @NotNull Duration maxDelay) {
}