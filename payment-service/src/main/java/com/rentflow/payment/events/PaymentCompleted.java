package com.rentflow.payment.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCompleted(
    UUID paymentId,
    String transactionId,
    BigDecimal settledAmount,
    BigDecimal feeAmount,
    String settlementMethod,
    Instant settledAt,
    Instant timestamp,
    Integer version
) {}
