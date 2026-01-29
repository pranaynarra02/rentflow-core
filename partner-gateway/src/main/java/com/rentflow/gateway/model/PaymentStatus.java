package com.rentflow.gateway.model;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    REQUIRES_ACTION,
    REQUIRES_CONFIRMATION
}
