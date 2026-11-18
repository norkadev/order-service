package com.norkadev.resilience.order.outbox;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;

import com.norkadev.resilience.order.messaging.event.OrderEvent;

@Service
@Transactional(readOnly = true)
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, JsonMapper jsonMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(OrderEvent event, String routingKey) {
        String payload = jsonMapper.writeValueAsString(event);
        outboxEventRepository.save(new OutboxEvent(
                event.eventId(),
                event.orderId(),
                event.eventType(),
                routingKey,
                payload,
                Instant.now()));
    }

    @Transactional
    public void markPublished(UUID eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Outbox event not found: " + eventId));
        event.markPublished(Instant.now());
    }
}
