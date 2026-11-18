package com.norkadev.resilience.order.outbox;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_event", indexes = {
        @Index(name = "idx_outbox_pending", columnList = "published_at, created_at")
})
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 128)
    private String routingKey;

    @Column(nullable = false, length = 4000)
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(
            UUID id,
            UUID aggregateId,
            String eventType,
            String routingKey,
            String payload,
            Instant createdAt) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.routingKey = routingKey;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
