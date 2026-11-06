package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.inbox;

import com.rubenmarin.paymentservice.application.model.InboxEvent;

import java.time.Instant;
import java.util.UUID;

public class InboxMapper {

    public static InboxEvent toDomain(InboxJpaEntity inboxJpaEntity) {
        UUID integrationEventId = inboxJpaEntity.getIntegrationEventId();
        Instant receivedAt = inboxJpaEntity.getReceivedAt();
        return new InboxEvent(integrationEventId, receivedAt);
    }

    public static InboxJpaEntity toEntity(InboxEvent inboxEvent) {
        UUID integrationEventId = inboxEvent.integrationEventId();
        Instant receivedAt = inboxEvent.receivedAt();
        return new InboxJpaEntity(integrationEventId, receivedAt);
    }
}
