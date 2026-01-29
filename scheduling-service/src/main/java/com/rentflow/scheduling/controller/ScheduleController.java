package com.rentflow.scheduling.controller;

import com.rentflow.scheduling.dto.CreateScheduleRequest;
import com.rentflow.scheduling.dto.ScheduleResponse;
import com.rentflow.scheduling.service.SchedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@Tag(name = "Payment Schedules", description = "Recurring payment schedule management")
public class ScheduleController {

    private final SchedulingService schedulingService;

    @PostMapping
    @Operation(summary = "Create a new payment schedule")
    public ResponseEntity<ScheduleResponse> createSchedule(@Valid @RequestBody CreateScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schedulingService.createSchedule(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get schedule by ID")
    public ResponseEntity<ScheduleResponse> getSchedule(@PathVariable UUID id) {
        return ResponseEntity.ok(schedulingService.getSchedule(id));
    }

    @GetMapping("/tenant/{tenantId}")
    @Operation(summary = "Get schedules by tenant")
    public ResponseEntity<List<ScheduleResponse>> getSchedulesByTenant(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(schedulingService.getSchedulesByTenant(tenantId));
    }

    @GetMapping("/lease/{leaseId}")
    @Operation(summary = "Get schedules by lease")
    public ResponseEntity<List<ScheduleResponse>> getSchedulesByLease(@PathVariable UUID leaseId) {
        return ResponseEntity.ok(schedulingService.getSchedulesByLease(leaseId));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause a payment schedule")
    public ResponseEntity<ScheduleResponse> pauseSchedule(
        @PathVariable UUID id,
        @RequestBody Map<String, String> request
    ) {
        return ResponseEntity.ok(schedulingService.pauseSchedule(id, request.get("reason")));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume a paused schedule")
    public ResponseEntity<ScheduleResponse> resumeSchedule(@PathVariable UUID id) {
        return ResponseEntity.ok(schedulingService.resumeSchedule(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a payment schedule")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID id) {
        schedulingService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "scheduling-service"));
    }
}
