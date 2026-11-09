package com.rubenmarin.paymentservice.application.model;

import com.rubenmarin.paymentservice.domain.model.PaymentStatus;

import java.util.Objects;
import java.util.UUID;

public record IntegrationEvent(
        UUID integrationEventId,
        UUID paymentRequestId,
        UUID bookingId,
        UUID paymentId,
        PaymentStatus status
        ) {

    public  IntegrationEvent {
        Objects.requireNonNull(integrationEventId);
        Objects.requireNonNull(paymentRequestId);
        Objects.requireNonNull(bookingId);
        Objects.requireNonNull(paymentId);
        Objects.requireNonNull(status);
        if (status == PaymentStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Integration event cannot have PENDING status"
            );
        }
    }
}
