package com.norkadev.resilience.order.messaging.event;

import java.time.Instant;
import java.util.UUID;

public sealed interface OrderEvent permits OrderCreated, OrderCancelled, StockReserved, StockRejected {

    UUID eventId();

    String eventType();

    Instant occurredAt();

    UUID orderId();
}
