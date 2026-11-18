package com.norkadev.resilience.order.dto;

import java.time.Instant;
import java.util.UUID;

import com.norkadev.resilience.order.domain.OrderStatus;

public record OrderResponse(
        UUID id,
        String sku,
        int quantity,
        OrderStatus status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt) {
}
