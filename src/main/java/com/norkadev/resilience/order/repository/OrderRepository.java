package com.norkadev.resilience.order.repository;

import java.util.UUID;

import com.norkadev.resilience.order.domain.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {
}
