package com.rubenmarin.paymentservice.application.model;

import java.time.Instant;
import java.util.UUID;

public record InboxEvent(
        UUID integrationEventId,
        Instant receivedAt
) {
}
