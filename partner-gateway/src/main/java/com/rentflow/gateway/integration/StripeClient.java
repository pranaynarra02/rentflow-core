package com.rentflow.gateway.integration;

import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.stripe.Stripe;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class StripeClient {

    @Value("${partners.stripe.api-key}")
    private String apiKey;

    @jakarta.annotation.PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }

    public PaymentIntentResult createPaymentIntent(
        UUID paymentId,
        BigDecimal amount,
        String currency,
        String paymentMethodId
    ) {
        try {
            long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(currency.toLowerCase())
                .setPaymentMethod(paymentMethodId)
                .setConfirm(true)
                .setOffSession(true)
                .putMetadata("paymentId", paymentId.toString())
                .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            log.info("Created Stripe PaymentIntent: {} for payment: {}",
                paymentIntent.getId(), paymentId);

            return new PaymentIntentResult(
                paymentIntent.getId(),
                paymentIntent.getStatus(),
                paymentIntent.getAmount().longValue() / 100.0,
                null
            );
        } catch (Exception e) {
            log.error("Error creating Stripe PaymentIntent", e);
            throw new RuntimeException("Failed to create payment intent with Stripe", e);
        }
    }

    public PaymentIntentResult confirmPaymentIntent(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            paymentIntent = paymentIntent.confirm();

            return new PaymentIntentResult(
                paymentIntent.getId(),
                paymentIntent.getStatus(),
                paymentIntent.getAmount().longValue() / 100.0,
                null
            );
        } catch (Exception e) {
            log.error("Error confirming Stripe PaymentIntent", e);
            throw new RuntimeException("Failed to confirm payment intent", e);
        }
    }

    public PaymentIntentResult getPaymentIntent(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

            BigDecimal feeAmount = null;
            if (paymentIntent.getApplicationFeeAmount() != null) {
                feeAmount = BigDecimal.valueOf(paymentIntent.getApplicationFeeAmount())
                    .divide(new BigDecimal("100"));
            }

            return new PaymentIntentResult(
                paymentIntent.getId(),
                paymentIntent.getStatus(),
                paymentIntent.getAmount().longValue() / 100.0,
                feeAmount
            );
        } catch (Exception e) {
            log.error("Error retrieving Stripe PaymentIntent", e);
            throw new RuntimeException("Failed to retrieve payment intent", e);
        }
    }

    public record PaymentIntentResult(
        String paymentIntentId,
        String status,
        Double amount,
        BigDecimal feeAmount
    ) {}
}
