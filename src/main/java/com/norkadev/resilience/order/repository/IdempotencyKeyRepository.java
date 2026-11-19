package com.norkadev.resilience.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.norkadev.resilience.order.domain.IdempotencyKey;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
}
