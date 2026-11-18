package com.norkadev.resilience.order.outbox;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "app.outbox")
public record OutboxProperties(
        @Min(1) long pollIntervalMs,
        @Min(1) int batchSize,
        @NotNull Duration confirmTimeout) {
}