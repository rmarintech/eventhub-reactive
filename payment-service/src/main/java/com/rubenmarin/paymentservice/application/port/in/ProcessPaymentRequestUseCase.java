package com.rubenmarin.paymentservice.application.port.in;

public interface ProcessPaymentRequestUseCase {
    void processPaymentRequest(ProcessPaymentRequestCommand  processPaymentRequestCommand);
}
