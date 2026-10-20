package com.rubenmarin.bookingservice.application.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InboxEvent(
        UUID eventId,
        OffsetDateTime processedAt
) {
}
