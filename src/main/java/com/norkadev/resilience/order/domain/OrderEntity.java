package com.norkadev.resilience.order.domain;

import java.util.UUID;

import com.norkadev.resilience.order.exception.InvalidOrderStateException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "orders")
public class OrderEntity extends BaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OrderStatus status;

    @Column(length = 255)
    private String failureReason;

    @Version
    @Column(nullable = false)
    private Long version;

    protected OrderEntity() {
    }

    public OrderEntity(String sku, int quantity) {
        this.id = UUID.randomUUID();
        this.sku = sku;
        this.quantity = quantity;
        this.status = OrderStatus.PENDING;
    }

    public void confirm() {
        requirePending();
        status = OrderStatus.CONFIRMED;
        failureReason = null;
    }

    public void reject(String reason) {
        requirePending();
        status = OrderStatus.REJECTED;
        failureReason = reason;
    }

    public void cancel() {
        if (status != OrderStatus.PENDING && status != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException("Cannot cancel an order in state " + status);
        }
        status = OrderStatus.CANCELLED;
    }

    private void requirePending() {
        if (status != OrderStatus.PENDING) {
            throw new InvalidOrderStateException("Order must be PENDING but is " + status);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Long getVersion() {
        return version;
    }
}
