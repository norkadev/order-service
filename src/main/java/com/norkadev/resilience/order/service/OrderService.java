package com.norkadev.resilience.order.service;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.norkadev.resilience.order.domain.OrderEntity;
import com.norkadev.resilience.order.dto.CreateOrderRequest;
import com.norkadev.resilience.order.dto.OrderResponse;
import com.norkadev.resilience.order.exception.OrderNotFoundException;
import com.norkadev.resilience.order.messaging.config.RabbitTopology;
import com.norkadev.resilience.order.messaging.event.OrderCancelled;
import com.norkadev.resilience.order.outbox.OutboxService;
import com.norkadev.resilience.order.repository.OrderRepository;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final OutboxService outboxService;
    private final OrderCreationService orderCreationService;

    public OrderService(OrderRepository orderRepository, OutboxService outboxService,
            OrderCreationService orderCreationService) {
        this.orderRepository = orderRepository;
        this.outboxService = outboxService;
        this.orderCreationService = orderCreationService;
    }

    public OrderResponse createOrder(String idempotencyKey, CreateOrderRequest request) {
        try {
            return orderCreationService.create(idempotencyKey, request);
        } catch (DataIntegrityViolationException exception) {
            OrderResponse winner = resolveWinnerWithRetry(idempotencyKey, request);
            if (winner != null) {
                return winner;
            }
            throw exception;
        }
    }

    public Page<OrderResponse> findAll(Pageable pageable) {
        Pageable safePageable = pageable;
        if (pageable.getPageSize() > 100) {
            safePageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }
        return orderRepository.findAll(safePageable).map(this::toResponse);
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

    private OrderResponse resolveWinnerWithRetry(String idempotencyKey, CreateOrderRequest request) {
        int maxAttempts = 4;
        long waitMillis = 25L;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            OrderResponse winner = orderCreationService.findExisting(idempotencyKey, request);
            if (winner != null) {
                return winner;
            }
            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(waitMillis * attempt); // Espera progresiva: 25ms, 50ms, 75ms
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return null;
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
