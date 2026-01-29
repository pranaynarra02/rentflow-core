package com.rentflow.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public sealed interface LedgerEvent permits LedgerEntryCreated, LedgerEntrySettled {

    UUID entryId();
    String eventType();
    Instant timestamp();
    Integer version();
}

record LedgerEntryCreated(
    UUID entryId(),
    UUID paymentId,
    String tenantId,
    String propertyId,
    Account debitAccount,
    Account creditAccount,
    BigDecimal amount,
    String currency,
    String reference,
    String description,
    LedgerEntryType entryType,
    Instant timestamp,
    Integer version
) implements LedgerEvent {
    public String eventType() { return "ledger.entry.created"; }
    public LedgerEntryCreated {
        version = 1;
    }
}

record LedgerEntrySettled(
    UUID entryId(),
    UUID transactionId,
    BigDecimal settledAmount,
    Instant settledAt,
    Instant timestamp,
    Integer version
) implements LedgerEvent {
    public String eventType() { return "ledger.entry.settled"; }
    public LedgerEntrySettled {
        version = 1;
    }
}

record Account(
    String accountNumber,
    String accountType,
    String ownerId
) {}

enum LedgerEntryType {
    RENT_PAYMENT,
    PARTIAL_PAYMENT,
    LATE_FEE,
    SECURITY_DEPOSIT,
    REFUND,
    ADJUSTMENT
}
