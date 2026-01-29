package com.rentflow.gateway.integration;

import com.plaid.client.PlaidClient;
import com.plaid.client.request.PaymentInitiationCreateRequest;
import com.plaid.client.request.PaymentInitiationGetRequest;
import com.plaid.client.request.PaymentInitiationListRequest;
import com.plaid.client.response.PaymentInitiationCreateResponse;
import com.plaid.client.response.PaymentInitiationGetResponse;
import com.plaid.client.response.PaymentInitiationListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import retrofit2.Response;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaidClient {

    private final PlaidClient plaidClient;

    @Value("${partners.plaid.client-id}")
    private String clientId;

    @Value("${partners.plaid.secret}")
    private String secret;

    public String initiatePayment(
        UUID paymentId,
        BigDecimal amount,
        String accessToken,
        String accountId
    ) {
        try {
            var request = new PaymentInitiationCreateRequest(
                clientId,
                secret,
                new PaymentInitiationCreateRequest.PaymentAmount(
                    String.valueOf(amount),
                    "USD"
                ),
                new PaymentInitiationCreateRequest.PaymentInstruction(
                    accountId
                ),
                PaymentInitiationCreateRequest.PaymentType.IMMEDIATE
            );

            Response<PaymentInitiationCreateResponse> response =
                plaidClient.paymentInitiationCreate(request).execute();

            if (response.isSuccessful() && response.body() != null) {
                String plaidPaymentId = response.body().getPaymentId();
                log.info("Created Plaid payment: {} for payment: {}", plaidPaymentId, paymentId);
                return plaidPaymentId;
            } else {
                throw new RuntimeException("Plaid payment initiation failed: " + response.message());
            }
        } catch (IOException e) {
            log.error("Error initiating Plaid payment", e);
            throw new RuntimeException("Failed to initiate payment with Plaid", e);
        }
    }

    public PaymentStatus getPaymentStatus(String plaidPaymentId) {
        try {
            var request = new PaymentInitiationGetRequest(
                clientId,
                secret,
                plaidPaymentId
            );

            Response<PaymentInitiationGetResponse> response =
                plaidClient.paymentInitiationGet(request).execute();

            if (response.isSuccessful() && response.body() != null) {
                var status = response.body().getPaymentStatus();
                return mapPlaidStatus(status);
            } else {
                throw new RuntimeException("Failed to get Plaid payment status");
            }
        } catch (IOException e) {
            log.error("Error getting Plaid payment status", e);
            throw new RuntimeException("Failed to get payment status from Plaid", e);
        }
    }

    private PaymentStatus mapPlaidStatus(String plaidStatus) {
        return switch (plaidStatus.toUpperCase()) {
            case "PROCESSING" -> PaymentStatus.PROCESSING;
            case "COMPLETED" -> PaymentStatus.COMPLETED;
            case "FAILED" -> PaymentStatus.FAILED;
            case "CANCELLED" -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.PENDING;
        };
    }
}
