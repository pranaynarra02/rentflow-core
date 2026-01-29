package com.rentflow.gateway.controller;

import com.rentflow.gateway.dto.PaymentGatewayRequest;
import com.rentflow.gateway.model.PaymentInitiation;
import com.rentflow.gateway.model.PaymentStatus;
import com.rentflow.gateway.service.PaymentGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Gateway", description = "External payment partner integrations")
public class PaymentGatewayController {

    private final PaymentGatewayService paymentGatewayService;

    @PostMapping("/initiate")
    @Operation(summary = "Initiate payment via gateway")
    public ResponseEntity<PaymentInitiation> initiatePayment(@Valid @RequestBody PaymentGatewayRequest request) {
        var initiation = paymentGatewayService.initiateStripePayment(
            request.paymentId(),
            request.tenantId(),
            request.leaseId(),
            request.amount(),
            request.currency(),
            request.paymentMethodId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(initiation);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment initiation by ID")
    public ResponseEntity<PaymentInitiation> getInitiation(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentGatewayService.getInitiation(id));
    }

    @GetMapping("/payment/{paymentId}")
    @Operation(summary = "Get initiation by payment ID")
    public ResponseEntity<PaymentInitiation> getInitiationByPaymentId(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentGatewayService.getInitiationByPaymentId(paymentId));
    }

    @PostMapping("/webhook/stripe")
    @Operation(summary = "Handle Stripe webhook")
    public ResponseEntity<String> handleStripeWebhook(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String signature
    ) {
        // Verify webhook signature and process event
        // This is a simplified version - in production, verify signature
        return ResponseEntity.ok("Webhook received");
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "partner-gateway"));
    }
}
