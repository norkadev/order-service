package com.norkadev.resilience.order.service;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.norkadev.resilience.order.domain.OrderEntity;
import com.norkadev.resilience.order.domain.OrderStatus;
import com.norkadev.resilience.order.domain.ProcessedEvent;
import com.norkadev.resilience.order.exception.OrderNotFoundException;
import com.norkadev.resilience.order.messaging.config.RabbitTopology;
import com.norkadev.resilience.order.messaging.event.OrderCancelled;
import com.norkadev.resilience.order.messaging.event.StockRejected;
import com.norkadev.resilience.order.messaging.event.StockReserved;
import com.norkadev.resilience.order.outbox.OutboxService;
import com.norkadev.resilience.order.repository.OrderRepository;
import com.norkadev.resilience.order.repository.ProcessedEventRepository;

@Service
@Transactional(readOnly = true)
public class InventoryEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryEventHandler.class);

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final OutboxService outboxService;

    public InventoryEventHandler(
            OrderRepository orderRepository,
            ProcessedEventRepository processedEventRepository,
            OutboxService outboxService) {
        this.orderRepository = orderRepository;
        this.processedEventRepository = processedEventRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public void handle(StockReserved event) {

        if (alreadyProcessed(event.eventId())) {
            return;
        }

        // Genero la excepción para que el evento vuelva a la cola y se reintente
        /*LOGGER.info("TEMP handle attempt");
        throw new IllegalStateException("boom");*/

         OrderEntity order = findOrder(event.orderId());
         processedEventRepository.save(new ProcessedEvent(event.eventId(),
         Instant.now()));
        
         if (order.getStatus() == OrderStatus.PENDING) {
            order.confirm();
         } else if (order.getStatus() == OrderStatus.CANCELLED) {
            outboxService.record(new OrderCancelled(
                UUID.randomUUID(),
                "OrderCancelled",
                Instant.now(),
                order.getId(),
                order.getSku(),
                order.getQuantity()), RabbitTopology.ORDER_CANCELLED_ROUTING_KEY);
          } else {
            LOGGER.info("Ignoring StockReserved event {} for order in state {}",
            event.eventId(), order.getStatus());
         }

    @Transactional
    public void handle(StockRejected event) {
        if (alreadyProcessed(event.eventId())) {
            return;
        }

        OrderEntity order = findOrder(event.orderId());
        processedEventRepository.save(new ProcessedEvent(event.eventId(), Instant.now()));
        if (order.getStatus() == OrderStatus.PENDING) {
            order.reject(event.reason());
        } else {
            LOGGER.info("Ignoring StockRejected event {} for order in state {}",
                    event.eventId(), order.getStatus());
        }
    }

    private boolean alreadyProcessed(UUID eventId) {
        if (processedEventRepository.existsById(eventId)) {
            LOGGER.info("Ignoring duplicate inventory event {}", eventId);
            return true;
        }
        return false;
    }

    private OrderEntity findOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
