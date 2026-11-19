package com.norkadev.resilience.order.exception;

public class IdempotencyKeyMissingException extends RuntimeException {

    public IdempotencyKeyMissingException() {
        super("The Idempotency-Key header is required");
    }
}
