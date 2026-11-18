package com.norkadev.resilience.order.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.norkadev.resilience.order.domain.OrderEntity;
import com.norkadev.resilience.order.domain.OrderStatus;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void savesAndReadsOrderFromH2() {
        UUID id = UUID.randomUUID();
        OrderEntity order = new OrderEntity(
                id, "SKU-001", 2, OrderStatus.PENDING, null, Instant.now());

        orderRepository.save(order);

        OrderEntity storedOrder = orderRepository.findById(id).orElseThrow();
        assertEquals("SKU-001", storedOrder.getSku());
        assertEquals(2, storedOrder.getQuantity());
        assertEquals(OrderStatus.PENDING, storedOrder.getStatus());
        assertTrue(storedOrder.getCreatedAt() != null);
        assertTrue(storedOrder.getUpdatedAt() != null);
        assertTrue(orderRepository.existsById(id));
    }
}
