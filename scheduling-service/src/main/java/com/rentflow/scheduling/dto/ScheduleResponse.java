package com.rentflow.scheduling.dto;

import com.rentflow.scheduling.model.PaymentMethod;
import com.rentflow.scheduling.model.RecurrencePattern;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ScheduleResponse(
    UUID id,
    UUID tenantId,
    UUID propertyId,
    UUID leaseId,
    String name,
    BigDecimal amount,
    String currency,
    PaymentMethod paymentMethod,
    RecurrencePattern recurrencePattern,
    Integer dayOfMonth,
    Integer dayOfWeek,
    LocalDate startDate,
    LocalDate endDate,
    Boolean active,
    Instant pausedAt,
    String pauseReason,
    Integer totalOccurrences,
    Integer completedOccurrences,
    Integer failedOccurrences,
    Instant nextExecutionTime,
    Instant lastExecutionTime,
    UUID lastPaymentId,
    String description,
    Boolean autoRetry,
    Instant createdAt,
    Instant updatedAt
) {}
