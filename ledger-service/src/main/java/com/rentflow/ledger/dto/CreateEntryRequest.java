package com.rentflow.ledger.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateEntryRequest(
    @NotNull(message = "Payment ID is required")
    UUID paymentId,

    @NotNull(message = "Tenant ID is required")
    UUID tenantId,

    @NotNull(message = "Property ID is required")
    UUID propertyId,

    @NotNull(message = "Lease ID is required")
    UUID leaseId,

    @NotNull(message = "Debit account is required")
    AccountInfo debitAccount,

    @NotNull(message = "Credit account is required")
    AccountInfo creditAccount,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 13, fraction = 2)
    BigDecimal amount,

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3-letter ISO code")
    String currency,

    @NotNull(message = "Entry type is required")
    com.rentflow.ledger.model.LedgerEntryType entryType,

    @Size(max = 100, message = "Reference too long")
    String reference,

    @Size(max = 500, message = "Description too long")
    String description
) {
    public record AccountInfo(
        @NotBlank(message = "Account number is required")
        String accountNumber,

        @NotBlank(message = "Account type is required")
        String accountType,

        @NotBlank(message = "Owner ID is required")
        String ownerId
    ) {}
}
