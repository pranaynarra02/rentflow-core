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
@Table(name = "accounts", indexes = {
    @Index(name = "idx_account_owner", columnList = "owner_id"),
    @Index(name = "idx_account_type", columnList = "account_type")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String accountNumber;

    @Column(nullable = false, length = 100)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountSubType accountSubType;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(nullable = false)
    private String currency = "USD";

    @Column
    private Boolean active = true;

    @Column(length = 500)
    private String description;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    @Builder.Default
    private Integer version = 0;

    public void debit(BigDecimal amount) {
        if (accountType == AccountType.ASSET || accountType == AccountType.EXPENSE) {
            currentBalance = currentBalance.add(amount);
        } else {
            currentBalance = currentBalance.subtract(amount);
        }
        updateAvailableBalance();
    }

    public void credit(BigDecimal amount) {
        if (accountType == AccountType.ASSET || accountType == AccountType.EXPENSE) {
            currentBalance = currentBalance.subtract(amount);
        } else {
            currentBalance = currentBalance.add(amount);
        }
        updateAvailableBalance();
    }

    private void updateAvailableBalance() {
        availableBalance = currentBalance;
    }
}
