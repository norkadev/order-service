package com.norkadev.resilience.order.messaging.event;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCreated(
        UUID eventId,
        String eventType,
        @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC") Instant occurredAt,
        UUID orderId,
        String sku,
        int quantity) implements OrderEvent {
}
