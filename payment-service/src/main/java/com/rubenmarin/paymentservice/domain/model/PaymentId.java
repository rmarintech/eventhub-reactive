package com.rubenmarin.paymentservice.domain.model;

import java.util.UUID;

public record PaymentId(UUID paymentId) {

    public PaymentId {
        if (paymentId == null) {
            throw new IllegalStateException("PaymentId cannot be null");
        }
    }

    public static PaymentId generatePaymentId() {
        return new PaymentId(UUID.randomUUID());
    }
}

