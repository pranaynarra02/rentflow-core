package com.rentflow.scheduling.service;

import com.rentflow.scheduling.events.PaymentCreated;
import com.rentflow.scheduling.events.PaymentScheduled;
import com.rentflow.scheduling.dto.CreateScheduleRequest;
import com.rentflow.scheduling.dto.ScheduleResponse;
import com.rentflow.scheduling.kafka.ScheduleEventPublisher;
import com.rentflow.scheduling.model.PaymentSchedule;
import com.rentflow.scheduling.repository.PaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulingService {

    private final PaymentScheduleRepository scheduleRepository;
    private final ScheduleEventPublisher eventPublisher;

    @Transactional
    public ScheduleResponse createSchedule(CreateScheduleRequest request) {
        var schedule = PaymentSchedule.builder()
            .id(UUID.randomUUID())
            .tenantId(request.tenantId())
            .propertyId(request.propertyId())
            .leaseId(request.leaseId())
            .name(request.name())
            .amount(request.amount())
            .currency(request.currency())
            .paymentMethod(request.paymentMethod())
            .bankAccountId(request.bankAccountId())
            .plaidProcessorToken(request.plaidProcessorToken())
            .recurrencePattern(request.recurrencePattern())
            .dayOfMonth(request.dayOfMonth())
            .dayOfWeek(request.dayOfWeek())
            .startDate(request.startDate())
            .endDate(request.endDate())
            .totalOccurrences(request.totalOccurrences())
            .description(request.description())
            .autoRetry(request.autoRetry() != null ? request.autoRetry() : true)
            .maxRetries(request.maxRetries() != null ? request.maxRetries() : 3)
            .nextExecutionTime(calculateFirstExecution(request))
            .build();

        schedule = scheduleRepository.save(schedule);

        // Publish scheduled event
        eventPublisher.publishScheduleCreated(schedule);

        log.info("Created payment schedule: {} for lease: {}", schedule.getId(), request.leaseId());
        return toResponse(schedule);
    }

    private Instant calculateFirstExecution(CreateScheduleRequest request) {
        var start = request.startDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        if (start.isBefore(Instant.now())) {
            return Instant.now();
        }
        return start;
    }

    @Transactional(readOnly = true)
    public ScheduleResponse getSchedule(UUID id) {
        var schedule = scheduleRepository.findById(id)
            .orElseThrow(() -> new ScheduleNotFoundException(id));
        return toResponse(schedule);
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getSchedulesByTenant(UUID tenantId) {
        return scheduleRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getSchedulesByLease(UUID leaseId) {
        return scheduleRepository.findByLeaseIdOrderByCreatedAtDesc(leaseId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public ScheduleResponse pauseSchedule(UUID id, String reason) {
        var schedule = scheduleRepository.findById(id)
            .orElseThrow(() -> new ScheduleNotFoundException(id));

        schedule.pause(reason);
        scheduleRepository.save(schedule);

        log.info("Paused schedule: {} - Reason: {}", id, reason);
        return toResponse(schedule);
    }

    @Transactional
    public ScheduleResponse resumeSchedule(UUID id) {
        var schedule = scheduleRepository.findById(id)
            .orElseThrow(() -> new ScheduleNotFoundException(id));

        schedule.resume();
        scheduleRepository.save(schedule);

        log.info("Resumed schedule: {}", id);
        return toResponse(schedule);
    }

    @Transactional
    public void deleteSchedule(UUID id) {
        var schedule = scheduleRepository.findById(id)
            .orElseThrow(() -> new ScheduleNotFoundException(id));

        scheduleRepository.delete(schedule);
        log.info("Deleted schedule: {}", id);
    }

    @Scheduled(cron = "${scheduling.execution.cron:0 */5 * * * *}") // Every 5 minutes
    @Transactional
    public void executeDueSchedules() {
        var schedules = scheduleRepository.findSchedulesReadyForExecution(Instant.now());

        log.info("Found {} schedules ready for execution", schedules.size());

        for (var schedule : schedules) {
            try {
                executeSchedule(schedule);
            } catch (Exception e) {
                log.error("Failed to execute schedule: {}", schedule.getId(), e);
                schedule.markExecutionFailed();
                scheduleRepository.save(schedule);
            }
        }
    }

    private void executeSchedule(PaymentSchedule schedule) {
        // Publish payment creation event to trigger payment service
        var paymentEvent = new PaymentCreated(
            UUID.randomUUID(),
            schedule.getTenantId(),
            schedule.getPropertyId(),
            schedule.getLeaseId(),
            schedule.getAmount(),
            schedule.getCurrency(),
            schedule.getPaymentMethod().name(),
            "RECURRING",
            Instant.now(),
            Instant.now(),
            1
        );

        eventPublisher.publishPaymentCreated(paymentEvent, schedule.getId());

        // Update schedule
        schedule.markExecutionCompleted(paymentEvent.paymentId());
        scheduleRepository.save(schedule);

        log.info("Executed schedule: {}, payment created: {}", schedule.getId(), paymentEvent.paymentId());
    }

    private ScheduleResponse toResponse(PaymentSchedule schedule) {
        return new ScheduleResponse(
            schedule.getId(),
            schedule.getTenantId(),
            schedule.getPropertyId(),
            schedule.getLeaseId(),
            schedule.getName(),
            schedule.getAmount(),
            schedule.getCurrency(),
            schedule.getPaymentMethod(),
            schedule.getRecurrencePattern(),
            schedule.getDayOfMonth(),
            schedule.getDayOfWeek(),
            schedule.getStartDate(),
            schedule.getEndDate(),
            schedule.getActive(),
            schedule.getPausedAt(),
            schedule.getPauseReason(),
            schedule.getTotalOccurrences(),
            schedule.getCompletedOccurrences(),
            schedule.getFailedOccurrences(),
            schedule.getNextExecutionTime(),
            schedule.getLastExecutionTime(),
            schedule.getLastPaymentId(),
            schedule.getDescription(),
            schedule.getAutoRetry(),
            schedule.getCreatedAt(),
            schedule.getUpdatedAt()
        );
    }
}
