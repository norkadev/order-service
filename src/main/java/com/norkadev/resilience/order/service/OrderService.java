package com.norkadev.resilience.order.service;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.norkadev.resilience.order.domain.OrderEntity;
import com.norkadev.resilience.order.dto.CreateOrderRequest;
import com.norkadev.resilience.order.dto.OrderResponse;
import com.norkadev.resilience.order.exception.OrderNotFoundException;
import com.norkadev.resilience.order.messaging.config.RabbitTopology;
import com.norkadev.resilience.order.messaging.event.OrderCancelled;
import com.norkadev.resilience.order.messaging.event.OrderCreated;
import com.norkadev.resilience.order.outbox.OutboxService;
import com.norkadev.resilience.order.repository.OrderRepository;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final OutboxService outboxService;

    public OrderService(OrderRepository orderRepository, OutboxService outboxService) {
        this.orderRepository = orderRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        OrderEntity order = orderRepository.save(new OrderEntity(request.sku(), request.quantity()));
        outboxService.record(new OrderCreated(
                UUID.randomUUID(),
                "OrderCreated",
                Instant.now(),
                order.getId(),
                order.getSku(),
                order.getQuantity()), RabbitTopology.ORDER_CREATED_ROUTING_KEY);
        OrderResponse response = toResponse(order);
        LOGGER.info("Created order {} for SKU {}", response.id(), response.sku());
        return response;
    }

    public Page<OrderResponse> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    public OrderResponse findById(UUID id) {
        return toResponse(findOrder(id));
    }

    @Transactional
    public OrderResponse cancelOrder(UUID id) {
        OrderEntity order = findOrder(id);
        order.cancel();
        outboxService.record(new OrderCancelled(
                UUID.randomUUID(),
                "OrderCancelled",
                Instant.now(),
                order.getId(),
                order.getSku(),
                order.getQuantity()), RabbitTopology.ORDER_CANCELLED_ROUTING_KEY);
        OrderResponse response = toResponse(orderRepository.save(order));
        LOGGER.info("Cancelled order {}", id);
        return response;
    }

    private OrderEntity findOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    private OrderResponse toResponse(OrderEntity order) {
        return new OrderResponse(
                order.getId(),
                order.getSku(),
                order.getQuantity(),
                order.getStatus(),
                order.getFailureReason(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }
}
