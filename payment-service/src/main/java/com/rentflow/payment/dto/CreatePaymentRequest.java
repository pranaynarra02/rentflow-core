package com.rentflow.payment.dto;

import com.rentflow.events.PaymentMethod;
import com.rentflow.events.PaymentType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreatePaymentRequest(
    @NotNull(message = "Tenant ID is required")
    UUID tenantId,

    @NotNull(message = "Property ID is required")
    UUID propertyId,

    @NotNull(message = "Lease ID is required")
    UUID leaseId,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 13, fraction = 2, message = "Invalid amount format")
    BigDecimal amount,

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter ISO code")
    String currency,

    @NotNull(message = "Payment type is required")
    PaymentType paymentType,

    @NotNull(message = "Payment method is required")
    PaymentMethod paymentMethod,

    String bankAccountId,
    String stripePaymentMethodId,
    String plaidProcessorToken,

    @Future(message = "Scheduled date must be in the future")
    Instant scheduledFor,

    @Size(max = 500, message = "Description too long")
    String description,

    Boolean partialPayment,
    UUID parentPaymentId,
    UUID idempotencyKey
) {}
