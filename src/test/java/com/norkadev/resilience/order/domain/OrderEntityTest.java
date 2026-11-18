package com.norkadev.resilience.order.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.norkadev.resilience.order.exception.InvalidOrderStateException;

class OrderEntityTest {

    @Test
    void confirmsAndRejectsOnlyPendingOrders() {
        OrderEntity order = orderIn(OrderStatus.PENDING);

        order.confirm();

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertNull(order.getFailureReason());
        assertThrows(InvalidOrderStateException.class, order::confirm);

        OrderEntity rejectedOrder = orderIn(OrderStatus.PENDING);
        rejectedOrder.reject("INSUFFICIENT_STOCK");
        assertEquals(OrderStatus.REJECTED, rejectedOrder.getStatus());
        assertEquals("INSUFFICIENT_STOCK", rejectedOrder.getFailureReason());
        assertThrows(InvalidOrderStateException.class, rejectedOrder::cancel);
    }

    @Test
    void cancelsPendingAndConfirmedOrdersOnly() {
        OrderEntity pending = orderIn(OrderStatus.PENDING);
        pending.cancel();
        assertEquals(OrderStatus.CANCELLED, pending.getStatus());

        OrderEntity confirmed = orderIn(OrderStatus.CONFIRMED);
        confirmed.cancel();
        assertEquals(OrderStatus.CANCELLED, confirmed.getStatus());

        assertThrows(InvalidOrderStateException.class, () -> orderIn(OrderStatus.REJECTED).cancel());
        assertThrows(InvalidOrderStateException.class, () -> orderIn(OrderStatus.CANCELLED).cancel());
    }

    private OrderEntity orderIn(OrderStatus status) {
        return new OrderEntity(UUID.randomUUID(), "SKU-001", 1, status, null, Instant.now());
    }
}
