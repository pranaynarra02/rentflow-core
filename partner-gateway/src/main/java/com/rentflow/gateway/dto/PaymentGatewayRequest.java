package com.rentflow.gateway.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentGatewayRequest(
    @NotNull(message = "Payment ID is required")
    UUID paymentId,

    @NotNull(message = "Tenant ID is required")
    UUID tenantId,

    @NotNull(message = "Lease ID is required")
    UUID leaseId,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 13, fraction = 2)
    BigDecimal amount,

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter ISO code")
    String currency,

    @NotBlank(message = "Payment method ID is required")
    String paymentMethodId,

    String processorToken,
    String bankAccountId
) {}
