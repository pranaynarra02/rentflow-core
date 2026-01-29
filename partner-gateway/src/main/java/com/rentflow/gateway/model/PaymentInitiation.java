package com.rentflow.gateway.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_initiations")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID paymentId;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID leaseId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentProvider provider;

    @Column(length = 100)
    private String bankAccountId;

    @Column(length = 100)
    private String processorToken;

    @Column(length = 100)
    private String paymentMethodId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(length = 100)
    private String externalTransactionId;

    @Column(length = 100)
    private String stripePaymentIntentId;

    @Column(length = 100)
    private String plaidPaymentId;

    @Column(precision = 19, scale = 2)
    private BigDecimal settledAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal feeAmount;

    @Column(length = 500)
    private String failureReason;

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
}
