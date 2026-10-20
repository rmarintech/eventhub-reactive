package com.rubenmarin.bookingservice.application.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OutboxEvent(
        OutboxEventId id,
        BrokerName brokerName,
        String eventType,
        UUID aggregateId,
        String topic,
        String payload,
        OutboxEventStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime publishedAt
) {

    public static OutboxEvent pending(
            BrokerName brokerName,
            String eventType,
            UUID aggregateId,
            String topic,
            String payload) {

        return new OutboxEvent(
                new OutboxEventId(UUID.randomUUID()),
                brokerName,
                eventType,
                aggregateId,
                topic,
                payload,
                OutboxEventStatus.PENDING,
                OffsetDateTime.now(),
                null
        );
    }

    public OutboxEvent markPublished() {
        if (status != OutboxEventStatus.PENDING) {
            throw new IllegalStateException("Only pending events can be published");
        }

        return new OutboxEvent(
                id,
                brokerName,
                eventType,
                aggregateId,
                topic,
                payload,
                OutboxEventStatus.PUBLISHED,
                createdAt,
                OffsetDateTime.now()
        );
    }
}
