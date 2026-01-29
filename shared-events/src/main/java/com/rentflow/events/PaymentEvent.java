package com.rentflow.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public sealed interface PaymentEvent permits PaymentCreated, PaymentCompleted, PaymentFailed, PaymentScheduled {

    UUID paymentId();
    String eventType();
    Instant timestamp();
    Integer version();
}

record PaymentCreated(
    UUID paymentId,
    String tenantId,
    String propertyId,
    String leaseId,
    BigDecimal amount,
    String currency,
    PaymentMethod paymentMethod,
    PaymentType paymentType,
    Instant scheduledFor,
    Instant timestamp,
    Integer version
) implements PaymentEvent {
    public String eventType() { return "payment.created"; }
    public PaymentCreated {
        version = 1;
    }
}

record PaymentCompleted(
    UUID paymentId,
    String transactionId,
    BigDecimal settledAmount,
    BigDecimal feeAmount,
    String settlementMethod,
    Instant settledAt,
    Instant timestamp,
    Integer version
) implements PaymentEvent {
    public String eventType() { return "payment.completed"; }
    public PaymentCompleted {
        version = 1;
    }
}

record PaymentFailed(
    UUID paymentId,
    String errorCode,
    String errorMessage,
    boolean retryable,
    Instant retryAfter,
    Instant timestamp,
    Integer version
) implements PaymentEvent {
    public String eventType() { return "payment.failed"; }
    public PaymentFailed {
        version = 1;
    }
}

record PaymentScheduled(
    UUID paymentId,
    UUID scheduleId,
    String recurrencePattern,
    Instant nextExecution,
    Instant timestamp,
    Integer version
) implements PaymentEvent {
    public String eventType() { return "payment.scheduled"; }
    public PaymentScheduled {
        version = 1;
    }
}

enum PaymentMethod {
    BANK_TRANSFER,
    CARD,
    ACH,
    WALLET
}

enum PaymentType {
    ONE_TIME,
    RECURRING,
    PARTIAL,
    FULL
}
