package com.rentflow.scheduling.repository;

import com.rentflow.scheduling.model.PaymentSchedule;
import com.rentflow.scheduling.model.RecurrencePattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, UUID> {

    List<PaymentSchedule> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<PaymentSchedule> findByLeaseIdOrderByCreatedAtDesc(UUID leaseId);

    List<PaymentSchedule> findByActiveTrueAndNextExecutionTimeBefore(Instant now);

    @Query("""
        SELECT s FROM PaymentSchedule s
        WHERE s.active = true
        AND s.nextExecutionTime IS NOT NULL
        AND s.nextExecutionTime < :now
        ORDER BY s.nextExecutionTime ASC
        """)
    List<PaymentSchedule> findSchedulesReadyForExecution(@org.springframework.data.annotation.QueryParam("now") Instant now);

    @Query("""
        SELECT COUNT(s) FROM PaymentSchedule s
        WHERE s.leaseId = :leaseId
        AND s.active = true
        """)
    long countActiveByLeaseId(UUID leaseId);

    List<PaymentSchedule> findByActiveTrue();
}
