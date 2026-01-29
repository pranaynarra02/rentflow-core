package com.rentflow.payment.service;

import com.rentflow.payment.model.Payment;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@CircuitBreaker(name = "partnerGateway")
@Retry(name = "partnerGateway")
@TimeLimiter(name = "partnerGateway")
public class PartnerGatewayClient {

    private final WebClient webClient;

    public PartnerGatewayClient(
        @Value("${services.partner-gateway.url:http://localhost:8083}") String baseUrl
    ) {
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    public PaymentResult initiatePayment(Payment payment) {
        var request = new PaymentRequest(
            payment.getId(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getPaymentMethod().name(),
            payment.getBankAccountId(),
            payment.getPlaidProcessorToken(),
            payment.getTenantId(),
            payment.getLeaseId()
        );

        return webClient.post()
            .uri("/api/v1/payments/initiate")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PaymentResult.class)
            .block();
    }

    public record PaymentRequest(
        UUID paymentId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String bankAccountId,
        String processorToken,
        UUID tenantId,
        UUID leaseId
    ) {}
}

record PaymentResult(
    String transactionId,
    BigDecimal settledAmount,
    BigDecimal feeAmount,
    String status
) {}
