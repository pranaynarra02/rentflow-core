package com.rentflow.payment.controller;

import com.rentflow.payment.dto.CreatePaymentRequest;
import com.rentflow.payment.dto.PaymentResponse;
import com.rentflow.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management APIs")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Create a new payment")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        var payment = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    @PostMapping("/{id}/process")
    @Operation(summary = "Process a payment")
    public ResponseEntity<PaymentResponse> processPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.processPayment(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a payment")
    public ResponseEntity<Void> cancelPayment(@PathVariable UUID id) {
        paymentService.cancelPayment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Get payments by tenant")
    public ResponseEntity<?> getPaymentsByTenant(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(paymentService.getPaymentsByTenant(tenantId, null));
    }

    @GetMapping("/lease/{leaseId}/total")
    @Operation(summary = "Get total paid amount for a lease")
    public ResponseEntity<Map<String, Object>> getTotalPaidForLease(@PathVariable UUID leaseId) {
        var total = paymentService.getTotalPaidForLease(leaseId);
        return ResponseEntity.ok(Map.of(
            "leaseId", leaseId,
            "totalPaid", total
        ));
    }

    @PostMapping("/scheduled/process")
    @Operation(summary = "Process scheduled payments (internal)")
    public ResponseEntity<Map<String, Object>> processScheduledPayments() {
        int processed = paymentService.processScheduledPayments();
        return ResponseEntity.ok(Map.of("processed", processed));
    }

    @PostMapping("/failed/retry")
    @Operation(summary = "Retry failed payments (internal)")
    public ResponseEntity<Map<String, Object>> retryFailedPayments() {
        int retried = paymentService.retryFailedPayments();
        return ResponseEntity.ok(Map.of("retried", retried));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "payment-service"));
    }
}
