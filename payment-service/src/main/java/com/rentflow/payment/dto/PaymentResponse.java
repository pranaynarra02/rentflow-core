package com.rentflow.payment.dto;

import com.rentflow.events.PaymentMethod;
import com.rentflow.events.PaymentType;
import com.rentflow.payment.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    UUID tenantId,
    UUID propertyId,
    UUID leaseId,
    BigDecimal amount,
    String currency,
    PaymentType paymentType,
    PaymentMethod paymentMethod,
    PaymentStatus status,
    BigDecimal settledAmount,
    BigDecimal feeAmount,
    String transactionId,
    String failureReason,
    Integer retryCount,
    Instant scheduledFor,
    Instant completedAt,
    String description,
    Boolean partialPayment,
    UUID parentPaymentId,
    List<PartialPaymentSummary> partialPayments,
    Instant createdAt,
    Instant updatedAt
) {
    public record PartialPaymentSummary(
        UUID id,
        BigDecimal amount,
        PaymentStatus status
    ) {}
}
