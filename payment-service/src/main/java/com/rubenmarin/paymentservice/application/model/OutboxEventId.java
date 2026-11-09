package com.rubenmarin.paymentservice.application.model;

import java.util.UUID;

public record OutboxEventId(
        UUID outboxEventId
) {

    public OutboxEventId {
        if (outboxEventId == null) {
            throw new IllegalStateException("OutboxEventId cannot be null");
        }
    }

    public static OutboxEventId generate() {
        return new OutboxEventId(UUID.randomUUID());
    }
}
