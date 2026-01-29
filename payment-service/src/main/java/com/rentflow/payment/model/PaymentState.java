package com.rentflow.payment.model;

import com.rentflow.events.PaymentMethod;
import com.rentflow.events.PaymentType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_tenant", columnList = "tenant_id"),
    @Index(name = "idx_payments_lease", columnList = "lease_id"),
    @Index(name = "idx_payments_status", columnList = "status"),
    @Index(name = "idx_payments_scheduled", columnList = "scheduled_for")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID propertyId;

    @Column(nullable = false)
    private UUID leaseId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Column(length = 100)
    private String bankAccountId;

    @Column(length = 100)
    private String externalPaymentId;

    @Column(length = 100)
    private String stripePaymentIntentId;

    @Column(length = 100)
    private String plaidProcessorToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(precision = 19, scale = 2)
    private BigDecimal settledAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal feeAmount;

    @Column(length = 100)
    private String transactionId;

    @Column(length = 100)
    private String failureReason;

    @Column(nullable = false)
    private Integer retryCount = 0;

    @Column(nullable = false)
    private Integer maxRetries = 3;

    @Column
    private Instant retryAfter;

    @Column
    private Instant scheduledFor;

    @Column
    private Instant completedAt;

    @Column
    private UUID idempotencyKey;

    @Column(length = 500)
    private String description;

    @Column(length = 1000)
    private String metadata;

    @Column
    private Boolean partialPayment = false;

    @Column
    private UUID parentPaymentId;

    @Column
    @Builder.Default
    @OneToMany(mappedBy = "parentPayment", cascade = CascadeType.ALL)
    private List<Payment> partialPayments = new ArrayList<>();

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
        if (scheduledFor == null) {
            scheduledFor = Instant.now();
        }
        if (idempotencyKey == null) {
            idempotencyKey = UUID.randomUUID();
        }
    }

    public boolean canRetry() {
        return retryCount < maxRetries &&
               (status == PaymentStatus.FAILED || status == PaymentStatus.PENDING) &&
               (retryAfter == null || retryAfter.isBefore(Instant.now()));
    }

    public void incrementRetry() {
        this.retryCount++;
        this.retryAfter = Instant.now().plusSeconds(calculateRetryDelay());
    }

    private long calculateRetryDelay() {
        return (long) Math.pow(2, retryCount) * 60; // Exponential backoff: 2min, 4min, 8min
    }

    public void markAsProcessing() {
        this.status = PaymentStatus.PROCESSING;
    }

    public void markAsCompleted(BigDecimal settledAmount, String transactionId) {
        this.status = PaymentStatus.COMPLETED;
        this.settledAmount = settledAmount;
        this.transactionId = transactionId;
        this.completedAt = Instant.now();
    }

    public void markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }
}
