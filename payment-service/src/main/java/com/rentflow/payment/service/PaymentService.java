package com.rentflow.payment.service;

import com.rentflow.events.*;
import com.rentflow.payment.dto.*;
import com.rentflow.payment.kafka.PaymentEventPublisher;
import com.rentflow.payment.model.Payment;
import com.rentflow.payment.model.PaymentStatus;
import com.rentflow.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;
    private final PartnerGatewayClient partnerGatewayClient;
    private final LedgerServiceClient ledgerServiceClient;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        // Check idempotency
        if (request.idempotencyKey() != null) {
            var existing = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Returning existing payment for idempotency key: {}", request.idempotencyKey());
                return toResponse(existing.get());
            }
        }

        // Handle partial payments
        Payment payment;
        if (Boolean.TRUE.equals(request.partialPayment()) && request.parentPaymentId() != null) {
            payment = createPartialPayment(request);
        } else {
            payment = createFullPayment(request);
        }

        payment = paymentRepository.save(payment);

        // Publish event
        eventPublisher.publishPaymentCreated(payment);

        log.info("Created payment: {}", payment.getId());
        return toResponse(payment);
    }

    private Payment createFullPayment(CreatePaymentRequest request) {
        return Payment.builder()
            .id(UUID.randomUUID())
            .tenantId(request.tenantId())
            .propertyId(request.propertyId())
            .leaseId(request.leaseId())
            .amount(request.amount())
            .currency(request.currency())
            .paymentType(request.paymentType())
            .paymentMethod(request.paymentMethod())
            .bankAccountId(request.bankAccountId())
            .plaidProcessorToken(request.plaidProcessorToken())
            .scheduledFor(request.scheduledFor() != null ? request.scheduledFor() : Instant.now())
            .description(request.description())
            .idempotencyKey(request.idempotencyKey())
            .partialPayment(request.partialPayment())
            .build();
    }

    private Payment createPartialPayment(CreatePaymentRequest request) {
        var parent = paymentRepository.findById(request.parentPaymentId())
            .orElseThrow(() -> new IllegalArgumentException("Parent payment not found"));

        var partial = Payment.builder()
            .id(UUID.randomUUID())
            .tenantId(request.tenantId())
            .propertyId(request.propertyId())
            .leaseId(request.leaseId())
            .amount(request.amount())
            .currency(request.currency())
            .paymentType(PaymentType.PARTIAL)
            .paymentMethod(request.paymentMethod())
            .bankAccountId(request.bankAccountId())
            .plaidProcessorToken(request.plaidProcessorToken())
            .scheduledFor(request.scheduledFor() != null ? request.scheduledFor() : Instant.now())
            .description(request.description())
            .partialPayment(true)
            .parentPaymentId(parent.getId())
            .build();

        parent.getPartialPayments().add(partial);
        return partial;
    }

    @Cacheable(value = "payments", key = "#id")
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID id) {
        var payment = paymentRepository.findById(id)
            .orElseThrow(() -> new PaymentNotFoundException(id));
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByTenant(UUID tenantId, Pageable pageable) {
        return paymentRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
            .stream()
            .map(this::toResponse)
            .collect(java.util.stream.Collectors.toCollection(() -> new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0)));
    }

    @Transactional
    @CacheEvict(value = "payments", key = "#id")
    public PaymentResponse processPayment(UUID id) {
        var payment = paymentRepository.findById(id)
            .orElseThrow(() -> new PaymentNotFoundException(id));

        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.FAILED) {
            throw new PaymentAlreadyProcessedException(id, payment.getStatus());
        }

        payment.markAsProcessing();
        paymentRepository.save(payment);

        try {
            // Initiate payment via partner gateway
            var result = partnerGatewayClient.initiatePayment(payment);

            // Create ledger entry
            ledgerServiceClient.createLedgerEntry(payment, result.transactionId());

            // Update payment
            payment.markAsCompleted(result.settledAmount(), result.transactionId());
            paymentRepository.save(payment);

            // Publish completion event
            eventPublisher.publishPaymentCompleted(payment);

            log.info("Successfully processed payment: {}", id);
            return toResponse(payment);

        } catch (Exception e) {
            payment.markAsFailed(e.getMessage());
            payment.incrementRetry();
            paymentRepository.save(payment);

            eventPublisher.publishPaymentFailed(payment, e);

            log.error("Failed to process payment: {}", id, e);
            throw new PaymentProcessingException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void cancelPayment(UUID id) {
        var payment = paymentRepository.findById(id)
            .orElseThrow(() -> new PaymentNotFoundException(id));

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            throw new PaymentCannotBeCancelledException(id);
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);

        log.info("Cancelled payment: {}", id);
    }

    @Transactional
    public int processScheduledPayments() {
        var scheduledPayments = paymentRepository.findByStatusAndScheduledForBefore(
            PaymentStatus.PENDING,
            Instant.now()
        );

        log.info("Found {} scheduled payments to process", scheduledPayments.size());

        int processed = 0;
        for (var payment : scheduledPayments) {
            try {
                processPayment(payment.getId());
                processed++;
            } catch (Exception e) {
                log.error("Failed to process scheduled payment: {}", payment.getId(), e);
            }
        }

        return processed;
    }

    @Transactional
    public int retryFailedPayments() {
        var retryablePayments = paymentRepository.findRetryablePayments(
            PaymentStatus.FAILED,
            Instant.now()
        );

        log.info("Found {} retryable payments", retryablePayments.size());

        int retried = 0;
        for (var payment : retryablePayments) {
            try {
                processPayment(payment.getId());
                retried++;
            } catch (Exception e) {
                log.error("Retry failed for payment: {}", payment.getId(), e);
            }
        }

        return retried;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPaidForLease(UUID leaseId) {
        return paymentRepository.sumSettledAmountByLeaseId(leaseId);
    }

    private PaymentResponse toResponse(Payment payment) {
        var partialSummaries = payment.getPartialPayments().stream()
            .map(pp -> new PaymentResponse.PartialPaymentSummary(
                pp.getId(),
                pp.getAmount(),
                pp.getStatus()
            ))
            .toList();

        return new PaymentResponse(
            payment.getId(),
            payment.getTenantId(),
            payment.getPropertyId(),
            payment.getLeaseId(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getPaymentType(),
            payment.getPaymentMethod(),
            payment.getStatus(),
            payment.getSettledAmount(),
            payment.getFeeAmount(),
            payment.getTransactionId(),
            payment.getFailureReason(),
            payment.getRetryCount(),
            payment.getScheduledFor(),
            payment.getCompletedAt(),
            payment.getDescription(),
            payment.getPartialPayment(),
            payment.getParentPaymentId(),
            partialSummaries,
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }
}
