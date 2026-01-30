package com.rentflow.payment.events;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailed(
    UUID paymentId,
    String errorCode,
    String errorMessage,
    boolean retryable,
    Instant retryAfter,
    Instant timestamp,
    Integer version
) {}
