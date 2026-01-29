package com.rentflow.scheduling.model;

import com.rentflow.scheduling.model.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

@Entity
@Table(name = "payment_schedules", indexes = {
    @Index(name = "idx_schedule_tenant", columnList = "tenant_id"),
    @Index(name = "idx_schedule_lease", columnList = "lease_id"),
    @Index(name = "idx_schedule_active", columnList = "active")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID propertyId;

    @Column(nullable = false)
    private UUID leaseId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Column(length = 100)
    private String bankAccountId;

    @Column(length = 100)
    private String plaidProcessorToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrencePattern recurrencePattern;

    @Column
    private Integer dayOfMonth; // For MONTHLY pattern (1-31)

    @Column
    private Integer dayOfWeek; // For WEEKLY pattern (1-7)

    @Column(nullable = false)
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column
    private Instant pausedAt;

    @Column
    private String pauseReason;

    @Column
    private Integer totalOccurrences;

    @Column
    private Integer completedOccurrences;

    @Column
    private Integer failedOccurrences;

    @Column
    private Instant nextExecutionTime;

    @Column
    private Instant lastExecutionTime;

    @Column
    private UUID lastPaymentId;

    @Column(length = 500)
    private String description;

    @Column
    private Boolean autoRetry = true;

    @Column
    private Integer maxRetries = 3;

    @Column
    private Boolean generatePartialPayment = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Integer version;

    public Instant calculateNextExecution() {
        LocalDate base = lastExecutionTime != null
            ? LocalDateTime.ofInstant(lastExecutionTime, ZoneOffset.UTC).toLocalDate()
            : startDate;

        LocalDate nextDate = switch (recurrencePattern) {
            case DAILY -> base.plusDays(1);
            case WEEKLY -> base.plusWeeks(1);
            case BI_WEEKLY -> base.plusWeeks(2);
            case MONTHLY -> calculateNextMonthly(base);
            case QUARTERLY -> base.plusMonths(3);
            case YEARLY -> base.plusYears(1);
        };

        // Check if we've passed the end date
        if (endDate != null && nextDate.isAfter(endDate)) {
            active = false;
            return null;
        }

        // Check if we've completed all occurrences
        if (totalOccurrences != null && completedOccurrences != null
            && completedOccurrences >= totalOccurrences) {
            active = false;
            return null;
        }

        return nextDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private LocalDate calculateNextMonthly(LocalDate base) {
        if (dayOfMonth != null) {
            // Try to use the specific day of month
            int day = Math.min(dayOfMonth, base.lengthOfMonth());
            LocalDate next = base.with(TemporalAdjusters.firstDayOfNextMonth()).withDayOfMonth(day);
            if (next.isBefore(base)) {
                next = next.plusMonths(1);
            }
            return next;
        } else {
            return base.plusMonths(1);
        }
    }

    public void markExecutionCompleted(UUID paymentId) {
        this.lastExecutionTime = Instant.now();
        this.lastPaymentId = paymentId;
        this.completedOccurrences++;
        this.nextExecutionTime = calculateNextExecution();
    }

    public void markExecutionFailed() {
        this.failedOccurrences++;
    }

    public void pause(String reason) {
        this.active = false;
        this.pausedAt = Instant.now();
        this.pauseReason = reason;
    }

    public void resume() {
        this.active = true;
        this.pausedAt = null;
        this.pauseReason = null;
        this.nextExecutionTime = calculateNextExecution();
    }
}
