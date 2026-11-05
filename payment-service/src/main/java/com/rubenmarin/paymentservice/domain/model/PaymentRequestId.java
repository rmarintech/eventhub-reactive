package com.rubenmarin.paymentservice.domain.model;

import java.util.UUID;

public record PaymentRequestId(UUID paymentRequestId) {

    public PaymentRequestId {
        if (paymentRequestId == null) {
            throw new IllegalStateException("PaymentRequestId cannot be null");
        }
    }

    public static PaymentRequestId generatePaymentRequestId() {
        return new PaymentRequestId(UUID.randomUUID());
    }
}

