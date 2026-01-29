package com.rentflow.ledger.controller;

import com.rentflow.ledger.dto.CreateEntryRequest;
import com.rentflow.ledger.model.LedgerEntry;
import com.rentflow.ledger.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/entries")
@RequiredArgsConstructor
@Tag(name = "Ledger", description = "Double-entry accounting ledger")
public class LedgerController {

    private final LedgerService ledgerService;

    @PostMapping
    @Operation(summary = "Create a ledger entry")
    public ResponseEntity<LedgerEntry> createEntry(@Valid @RequestBody CreateEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.createEntry(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ledger entry by ID")
    public ResponseEntity<LedgerEntry> getEntry(@PathVariable UUID id) {
        return ResponseEntity.ok(ledgerService.getEntry(id));
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Get entries by tenant")
    public ResponseEntity<List<LedgerEntry>> getEntriesByTenant(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(ledgerService.getEntriesByTenant(tenantId));
    }

    @GetMapping("/lease/{leaseId}")
    @Operation(summary = "Get entries by lease")
    public ResponseEntity<List<LedgerEntry>> getEntriesByLease(@PathVariable UUID leaseId) {
        return ResponseEntity.ok(ledgerService.getEntriesByLease(leaseId));
    }

    @GetMapping("/tenant/{tenantId}/revenue")
    @Operation(summary = "Get total revenue for tenant")
    public ResponseEntity<Map<String, Object>> getTotalRevenue(@PathVariable UUID tenantId) {
        BigDecimal total = ledgerService.getTotalRevenue(tenantId);
        return ResponseEntity.ok(Map.of("tenantId", tenantId, "totalRevenue", total));
    }

    @GetMapping("/lease/{leaseId}/total")
    @Operation(summary = "Get total paid for lease")
    public ResponseEntity<Map<String, Object>> getTotalPaidForLease(@PathVariable UUID leaseId) {
        BigDecimal total = ledgerService.getTotalPaidForLease(leaseId);
        return ResponseEntity.ok(Map.of("leaseId", leaseId, "totalPaid", total));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ledger-service"));
    }
}
