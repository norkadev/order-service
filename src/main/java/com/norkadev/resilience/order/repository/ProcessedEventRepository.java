package com.norkadev.resilience.order.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.norkadev.resilience.order.domain.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
