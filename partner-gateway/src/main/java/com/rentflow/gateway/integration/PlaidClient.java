package com.rentflow.gateway.integration;

import com.rentflow.gateway.model.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaidClient {

    @Value("${partners.plaid.client-id}")
    private String clientId;

    @Value("${partners.plaid.secret}")
    private String secret;

    @Value("${partners.plaid.environment}")
    private String environment;

    public String initiatePayment(
        UUID paymentId,
        BigDecimal amount,
        String accessToken,
        String accountId
    ) {
        // TODO: Implement Plaid payment initiation
        // This requires proper Plaid API setup with payment initiation
        log.info("Plaid payment initiation for payment: {} amount: {}", paymentId, amount);
        return "plaid_payment_" + paymentId;
    }

    public PaymentStatus getPaymentStatus(String plaidPaymentId) {
        // TODO: Implement actual Plaid status check
        log.info("Checking Plaid payment status for: {}", plaidPaymentId);
        return PaymentStatus.PENDING;
    }
}
