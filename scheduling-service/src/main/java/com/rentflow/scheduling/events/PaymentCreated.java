package com.rentflow.scheduling.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCreated(
    UUID paymentId,
    UUID tenantId,
    UUID propertyId,
    UUID leaseId,
    BigDecimal amount,
    String currency,
    String paymentMethod,
    String paymentType,
    Instant scheduledFor,
    Instant timestamp,
    Integer version
) {}
