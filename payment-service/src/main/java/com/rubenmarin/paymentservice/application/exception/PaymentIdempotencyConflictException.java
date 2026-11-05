package com.rubenmarin.paymentservice.application.exception;

public class PaymentIdempotencyConflictException extends RuntimeException {
    public PaymentIdempotencyConflictException(String message) {
        super(message);
    }
}
