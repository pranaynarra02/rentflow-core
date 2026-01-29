package com.rentflow.payment.service;

import com.rentflow.payment.model.Payment;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@CircuitBreaker(name = "ledgerService")
@Retry(name = "ledgerService")
@TimeLimiter(name = "ledgerService")
public class LedgerServiceClient {

    private final WebClient webClient;

    public LedgerServiceClient(
        @Value("${services.ledger.url:http://localhost:8084}") String baseUrl
    ) {
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    public void createLedgerEntry(Payment payment, String transactionId) {
        var request = new LedgerEntryRequest(
            payment.getId(),
            payment.getTenantId(),
            payment.getPropertyId(),
            payment.getLeaseId(),
            payment.getAmount(),
            payment.getCurrency(),
            transactionId,
            "Rent payment"
        );

        try {
            webClient.post()
                .uri("/api/v1/entries")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
            log.info("Created ledger entry for payment: {}", payment.getId());
        } catch (Exception e) {
            log.error("Failed to create ledger entry for payment: {}", payment.getId(), e);
            throw new RuntimeException("Ledger entry creation failed", e);
        }
    }

    public record LedgerEntryRequest(
        UUID paymentId,
        UUID tenantId,
        UUID propertyId,
        UUID leaseId,
        BigDecimal amount,
        String currency,
        String reference,
        String description
    ) {}
}
