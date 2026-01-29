package com.rentflow.gateway.service;

import com.rentflow.gateway.integration.StripeClient;
import com.rentflow.gateway.model.PaymentInitiation;
import com.rentflow.gateway.model.PaymentStatus;
import com.rentflow.gateway.repository.PaymentInitiationRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentGatewayService {

    private final PaymentInitiationRepository initiationRepository;
    private final StripeClient stripeClient;
    // private final PlaidClient plaidClient; // Uncomment when Plaid client is ready

    @Transactional
    @CircuitBreaker(name = "stripeApi")
    @Retry(name = "stripeApi")
    public PaymentInitiation initiateStripePayment(
        UUID paymentId,
        UUID tenantId,
        UUID leaseId,
        BigDecimal amount,
        String currency,
        String paymentMethodId
    ) {
        var initiation = PaymentInitiation.builder()
            .id(UUID.randomUUID())
            .paymentId(paymentId)
            .tenantId(tenantId)
            .leaseId(leaseId)
            .amount(amount)
            .currency(currency)
            .provider(com.rentflow.gateway.model.PaymentProvider.STRIPE)
            .paymentMethodId(paymentMethodId)
            .status(PaymentStatus.PROCESSING)
            .build();

        try {
            var result = stripeClient.createPaymentIntent(
                paymentId,
                amount,
                currency,
                paymentMethodId
            );

            initiation.setStripePaymentIntentId(result.paymentIntentId());
            initiation.setExternalTransactionId(result.paymentIntentId());
            initiation.setSettledAmount(BigDecimal.valueOf(result.amount()));
            initiation.setFeeAmount(result.feeAmount());

            if ("succeeded".equals(result.status())) {
                initiation.setStatus(PaymentStatus.COMPLETED);
            } else if ("requires_action".equals(result.status())) {
                initiation.setStatus(PaymentStatus.REQUIRES_ACTION);
            } else if ("requires_confirmation".equals(result.status())) {
                initiation.setStatus(PaymentStatus.REQUIRES_CONFIRMATION);
            }

        } catch (Exception e) {
            initiation.setStatus(PaymentStatus.FAILED);
            initiation.setFailureReason(e.getMessage());
            log.error("Failed to initiate Stripe payment for payment: {}", paymentId, e);
        }

        return initiationRepository.save(initiation);
    }

    @Transactional(readOnly = true)
    public PaymentInitiation getInitiation(UUID id) {
        return initiationRepository.findById(id)
            .orElseThrow(() -> new PaymentInitiationNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public PaymentInitiation getInitiationByPaymentId(UUID paymentId) {
        return initiationRepository.findByPaymentId(paymentId)
            .orElseThrow(() -> new PaymentInitiationNotFoundException(paymentId));
    }

    @Transactional
    public PaymentInitiation updateStatus(UUID id, PaymentStatus status) {
        var initiation = getInitiation(id);
        initiation.setStatus(status);
        return initiationRepository.save(initiation);
    }

    public static class PaymentInitiationNotFoundException extends RuntimeException {
        public PaymentInitiationNotFoundException(UUID id) {
            super("Payment initiation not found: " + id);
        }
    }
}
