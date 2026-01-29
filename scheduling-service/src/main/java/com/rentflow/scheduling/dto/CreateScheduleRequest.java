package com.rentflow.scheduling.dto;

import com.rentflow.events.PaymentMethod;
import com.rentflow.scheduling.model.RecurrencePattern;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateScheduleRequest(
    @NotNull(message = "Tenant ID is required")
    UUID tenantId,

    @NotNull(message = "Property ID is required")
    UUID propertyId,

    @NotNull(message = "Lease ID is required")
    UUID leaseId,

    @NotBlank(message = "Schedule name is required")
    @Size(max = 200, message = "Name too long")
    String name,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 13, fraction = 2)
    BigDecimal amount,

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$")
    String currency,

    @NotNull(message = "Payment method is required")
    PaymentMethod paymentMethod,

    String bankAccountId,
    String plaidProcessorToken,

    @NotNull(message = "Recurrence pattern is required")
    RecurrencePattern recurrencePattern,

    @Min(value = 1, message = "Day of month must be between 1 and 31")
    @Max(value = 31, message = "Day of month must be between 1 and 31")
    Integer dayOfMonth,

    @Min(value = 1, message = "Day of week must be between 1 and 7")
    @Max(value = 7, message = "Day of week must be between 1 and 7")
    Integer dayOfWeek,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    LocalDate endDate,

    @Min(value = 1, message = "Total occurrences must be at least 1")
    Integer totalOccurrences,

    @Size(max = 500)
    String description,

    Boolean autoRetry,
    Integer maxRetries
) {}
