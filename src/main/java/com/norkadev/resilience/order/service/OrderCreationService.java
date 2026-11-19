package com.norkadev.resilience.order.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.norkadev.resilience.order.domain.IdempotencyKey;
import com.norkadev.resilience.order.domain.OrderEntity;
import com.norkadev.resilience.order.dto.CreateOrderRequest;
import com.norkadev.resilience.order.dto.OrderResponse;
import com.norkadev.resilience.order.exception.IdempotencyConflictException;
import com.norkadev.resilience.order.exception.OrderNotFoundException;
import com.norkadev.resilience.order.messaging.config.RabbitTopology;
import com.norkadev.resilience.order.messaging.event.OrderCreated;
import com.norkadev.resilience.order.outbox.OutboxService;
import com.norkadev.resilience.order.repository.IdempotencyKeyRepository;
import com.norkadev.resilience.order.repository.OrderRepository;

@Service
@Transactional(readOnly = true)
public class OrderCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderCreationService.class);
    private final OrderRepository orderRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final OutboxService outboxService;

    public OrderCreationService(
            OrderRepository orderRepository,
            IdempotencyKeyRepository idempotencyKeyRepository,
            OutboxService outboxService) {
        this.orderRepository = orderRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public OrderResponse create(String idempotencyKey, CreateOrderRequest request) {
        String requestHash = hashRequest(request);
        var existing = idempotencyKeyRepository.findById(idempotencyKey);
        if (existing.isPresent()) {
            return existingResponse(existing.get(), requestHash);
        }

        OrderEntity order = orderRepository.save(new OrderEntity(request.sku(), request.quantity()));
        outboxService.record(new OrderCreated(
                UUID.randomUUID(),
                "OrderCreated",
                Instant.now(),
                order.getId(),
                order.getSku(),
                order.getQuantity()), RabbitTopology.ORDER_CREATED_ROUTING_KEY);
        idempotencyKeyRepository.saveAndFlush(new IdempotencyKey(idempotencyKey, requestHash, order.getId()));

        LOGGER.info("Created order {} for SKU {}", order.getId(), order.getSku());
        return toResponse(order);
    }

    public OrderResponse findExisting(String idempotencyKey, CreateOrderRequest request) {
        var existing = idempotencyKeyRepository.findById(idempotencyKey);
        return existing.map(key -> existingResponse(key, hashRequest(request))).orElse(null);
    }

    private OrderResponse existingResponse(IdempotencyKey idempotencyKey, String requestHash) {
        if (!idempotencyKey.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException();
        }
        OrderEntity order = orderRepository.findById(idempotencyKey.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(idempotencyKey.getOrderId()));
        return toResponse(order);
    }

    private String hashRequest(CreateOrderRequest request) {
        String body = request.sku() + "\n" + request.quantity();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(body.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
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
