package com.rentflow.ledger.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries", indexes = {
    @Index(name = "idx_entry_payment", columnList = "payment_id"),
    @Index(name = "idx_entry_account", columnList = "account_number"),
    @Index(name = "idx_entry_tenant", columnList = "tenant_id"),
    @Index(name = "idx_entry_date", columnList = "entry_date")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID paymentId;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID propertyId;

    @Column(nullable = false)
    private UUID leaseId;

    // Debit account (money going out)
    @Column(nullable = false, length = 50)
    private String debitAccountNumber;

    @Column(nullable = false, length = 50)
    private String debitAccountType;

    @Column(nullable = false, length = 50)
    private String debitAccountOwnerId;

    // Credit account (money coming in)
    @Column(nullable = false, length = 50)
    private String creditAccountNumber;

    @Column(nullable = false, length = 50)
    private String creditAccountType;

    @Column(nullable = false, length = 50)
    private String creditAccountOwnerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerEntryType entryType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryStatus status = EntryStatus.PENDING;

    @Column(length = 100)
    private String reference;

    @Column(length = 500)
    private String description;

    @Column
    private Instant entryDate;

    @Column
    private Instant postedDate;

    @Column(length = 100)
    private String transactionId;

    @Column(length = 100)
    private String batchId;

    @Column(precision = 19, scale = 2)
    private BigDecimal debitBalance;

    @Column(precision = 19, scale = 2)
    private BigDecimal creditBalance;

    @Column(length = 1000)
    private String metadata;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Integer version;

    @PrePersist
    protected void onCreate() {
        if (entryDate == null) {
            entryDate = Instant.now();
        }
    }

    public void postEntry(String transactionId) {
        this.status = EntryStatus.POSTED;
        this.transactionId = transactionId;
        this.postedDate = Instant.now();
    }

    public void settle(BigDecimal settledAmount) {
        this.status = EntryStatus.SETTLED;
        this.debitBalance = settledAmount;
        this.creditBalance = settledAmount;
    }
}
