package com.norkadev.resilience.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import com.norkadev.resilience.order.domain.OrderEntity;
import com.norkadev.resilience.order.domain.OrderStatus;
import com.norkadev.resilience.order.dto.CreateOrderRequest;
import com.norkadev.resilience.order.dto.InventoryResponse;
import com.norkadev.resilience.order.dto.OrderResponse;
import com.norkadev.resilience.order.exception.InsufficientStockException;
import com.norkadev.resilience.order.exception.OrderNotFoundException;
import com.norkadev.resilience.order.repository.OrderRepository;
import com.norkadev.resilience.order.service.client.InventoryServiceClient;

class OrderServiceTest {

    private InventoryServiceClient inventoryServiceClient;
    private OrderRepository orderRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        inventoryServiceClient = mock(InventoryServiceClient.class);
        orderRepository = mock(OrderRepository.class);
        orderService = new OrderService(inventoryServiceClient, orderRepository);
    }

    @Test
    void createsConfirmedOrderWhenInventoryHasEnoughStock() {
        when(inventoryServiceClient.getInventory("SKU-001"))
                .thenReturn(ResponseEntity.ok(new InventoryResponse("SKU-001", 5)));
        when(orderRepository.save(any(OrderEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse order = orderService.createOrder(new CreateOrderRequest("SKU-001", 2));

        assertEquals("SKU-001", order.sku());
        assertEquals(2, order.quantity());
        assertEquals(OrderStatus.CONFIRMED, order.status());
        verify(orderRepository).save(any(OrderEntity.class));
    }

    @Test
    void doesNotCreateOrderWhenInventoryIsInsufficient() {
        when(inventoryServiceClient.getInventory("SKU-001"))
                .thenReturn(ResponseEntity.ok(new InventoryResponse("SKU-001", 1)));

        assertThrows(
                InsufficientStockException.class,
                () -> orderService.createOrder(new CreateOrderRequest("SKU-001", 2)));

        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    @Test
    void cancelsExistingOrder() {
        UUID id = UUID.randomUUID();
        OrderEntity entity = new OrderEntity(
                id, "SKU-001", 1, OrderStatus.CONFIRMED, null, Instant.now());
        when(orderRepository.findById(id)).thenReturn(Optional.of(entity));
        when(orderRepository.save(entity)).thenReturn(entity);

        OrderResponse cancelled = orderService.cancelOrder(id);

        assertEquals(OrderStatus.CANCELLED, cancelled.status());
        verify(orderRepository).save(entity);
    }

    @Test
    void returnsPagedOrders() {
        OrderEntity entity = new OrderEntity(
                UUID.randomUUID(), "SKU-001", 1, OrderStatus.PENDING, null, Instant.now());
        when(orderRepository.findAll(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(java.util.List.of(entity)));

        var page = orderService.findAll(PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals(OrderStatus.PENDING, page.getContent().getFirst().status());
    }

    @Test
    void throwsNotFoundWhenCancellingMissingOrder() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.cancelOrder(id));
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }
}
