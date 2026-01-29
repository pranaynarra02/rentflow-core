package com.rentflow.scheduling.events;

import java.time.Instant;
import java.util.UUID;

public record PaymentScheduled(
    UUID paymentId,
    UUID scheduleId,
    String recurrencePattern,
    Instant nextExecution,
    Instant timestamp,
    Integer version
) {}
