package com.rubenmarin.eventhub.event.application.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InboxEvent(
        UUID integrationEventId,
        OffsetDateTime processedAt
) {
}
