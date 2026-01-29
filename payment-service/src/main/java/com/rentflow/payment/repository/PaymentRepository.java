package com.rentflow.payment.repository;

import com.rentflow.payment.model.Payment;
import com.rentflow.payment.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(UUID idempotencyKey);

    List<Payment> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<Payment> findByLeaseIdOrderByCreatedAtDesc(UUID leaseId);

    List<Payment> findByStatusAndScheduledForBefore(
        PaymentStatus status,
        Instant scheduledFor
    );

    List<Payment> findByParentPaymentId(UUID parentPaymentId);

    @Query("""
        SELECT p FROM Payment p
        WHERE p.status = :status
        AND p.retryAfter IS NULL OR p.retryAfter < :now
        AND p.retryCount < p.maxRetries
        ORDER BY p.scheduledFor ASC
        """)
    List<Payment> findRetryablePayments(
        @Param("status") PaymentStatus status,
        @Param("now") Instant now
    );

    @Query("""
        SELECT COUNT(p) FROM Payment p
        WHERE p.tenantId = :tenantId
        AND p.status = :status
        AND p.createdAt > :after
        """)
    long countByTenantIdAndStatusAndCreatedAtAfter(
        @Param("tenantId") UUID tenantId,
        @Param("status") PaymentStatus status,
        @Param("after") Instant after
    );

    @Query("""
        SELECT COALESCE(SUM(p.settledAmount), 0) FROM Payment p
        WHERE p.leaseId = :leaseId
        AND p.status = 'COMPLETED'
        """)
    java.math.BigDecimal sumSettledAmountByLeaseId(@Param("leaseId") UUID leaseId);
}
