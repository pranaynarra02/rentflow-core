package com.rentflow.ledger.model;

public enum AccountSubType {
    // Asset subtypes
    CASH,
    BANK_ACCOUNT,
    ACCOUNTS_RECEIVABLE,

    // Liability subtypes
    ACCOUNTS_PAYABLE,
    SECURITY_DEPOSIT_HELD,

    // Equity subtypes
    OWNER_EQUITY,
    RETAINED_EARNINGS,

    // Revenue subtypes
    RENT_INCOME,
    LATE_FEE_INCOME,

    // Expense subtypes
    MAINTENANCE_EXPENSE,
    PROPERTY_MANAGEMENT_FEE
}
