package com.rentflow.payment.exception;

import java.util.UUID;

public class PaymentCannotBeCancelledException extends RuntimeException {
    public PaymentCannotBeCancelledException(UUID id) {
        super("Completed payment cannot be cancelled: " + id);
    }
}
