package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.outbox;

import com.rubenmarin.paymentservice.application.model.OutboxEvent;
import com.rubenmarin.paymentservice.application.model.OutboxEventId;

public class OutboxMapper {

    public static OutboxEvent toDomain(OutboxJpaEntity outboxJpaEntity) {

        return new OutboxEvent(
                new OutboxEventId(outboxJpaEntity.getId()),
                outboxJpaEntity.getMessageKey(),
                outboxJpaEntity.getEventType(),
                outboxJpaEntity.getAggregateId(),
                outboxJpaEntity.getTopic(),
                outboxJpaEntity.getPayload(),
                outboxJpaEntity.getStatus(),
                outboxJpaEntity.getCreatedAt(),
                outboxJpaEntity.getPublishedAt()
        );

    }

    public static OutboxJpaEntity toEntity(OutboxEvent outboxEvent) {
        return new OutboxJpaEntity(
                outboxEvent.id().id(),
                outboxEvent.messageKey(),
                outboxEvent.eventType(),
                outboxEvent.aggregateId(),
                outboxEvent.topic(),
                outboxEvent.payload(),
                outboxEvent.status(),
                outboxEvent.createdAt(),
                outboxEvent.publishedAt()
        );
    }

}
