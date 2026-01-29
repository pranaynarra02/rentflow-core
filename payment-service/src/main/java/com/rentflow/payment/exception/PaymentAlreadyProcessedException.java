package com.rentflow.payment.exception;

import com.rentflow.payment.model.PaymentStatus;
import java.util.UUID;

public class PaymentAlreadyProcessedException extends RuntimeException {
    public PaymentAlreadyProcessedException(UUID id, PaymentStatus status) {
        super("Payment " + id + " already has status " + status);
    }
}
