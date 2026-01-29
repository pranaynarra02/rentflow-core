package com.rentflow.scheduling.exception;

import java.util.UUID;

public class ScheduleNotFoundException extends RuntimeException {
    public ScheduleNotFoundException(UUID id) {
        super("Payment schedule not found: " + id);
    }
}
