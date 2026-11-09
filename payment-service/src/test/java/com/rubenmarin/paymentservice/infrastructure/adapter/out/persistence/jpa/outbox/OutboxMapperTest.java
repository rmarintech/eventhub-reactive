package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.outbox;

import com.rubenmarin.paymentservice.application.model.OutboxEvent;
import com.rubenmarin.paymentservice.application.model.OutboxEventId;
import com.rubenmarin.paymentservice.application.model.OutboxEventStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class OutboxMapperTest {

    @Test
    void shouldMapOutboxEventToJpaEntity() {

        // Given
        UUID aggregateId = UUID.randomUUID();

        OutboxEvent outboxEvent = OutboxEvent.pending(
                OutboxEventId.generate(),
                "booking-123",
                "PaymentCompleted",
                aggregateId,
                "payment-completed",
                """
                {
                    "paymentId": "123",
                    "status": "COMPLETED"
                }
                """
        );

        // When
        OutboxJpaEntity entity = OutboxMapper.toEntity(outboxEvent);

        // Then
        Assertions.assertEquals(outboxEvent.id().outboxEventId(), entity.getId());
        Assertions.assertEquals(outboxEvent.messageKey(), entity.getMessageKey());
        Assertions.assertEquals(outboxEvent.eventType(), entity.getEventType());
        Assertions.assertEquals(outboxEvent.aggregateId(), entity.getAggregateId());
        Assertions.assertEquals(outboxEvent.topic(), entity.getTopic());
        Assertions.assertEquals(outboxEvent.payload(), entity.getPayload());
        Assertions.assertEquals(outboxEvent.status(), entity.getStatus());
        Assertions.assertEquals(outboxEvent.createdAt(), entity.getCreatedAt());
        Assertions.assertNull(entity.getPublishedAt());
    }

    @Test
    void shouldMapJpaEntityToOutboxEvent() {

        // Given
        UUID id = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();

        OutboxJpaEntity entity = new OutboxJpaEntity(
                id,
                "booking-123",
                "PaymentCompleted",
                aggregateId,
                "payment-completed",
                """
                {
                    "paymentId": "123",
                    "status": "COMPLETED"
                }
                """,
                OutboxEventStatus.PENDING,
                createdAt,
                null
        );

        // When
        OutboxEvent outboxEvent = OutboxMapper.toDomain(entity);

        // Then
        Assertions.assertEquals(new OutboxEventId(id), outboxEvent.id());
        Assertions.assertEquals(entity.getMessageKey(), outboxEvent.messageKey());
        Assertions.assertEquals(entity.getEventType(), outboxEvent.eventType());
        Assertions.assertEquals(entity.getAggregateId(), outboxEvent.aggregateId());
        Assertions.assertEquals(entity.getTopic(), outboxEvent.topic());
        Assertions.assertEquals(entity.getPayload(), outboxEvent.payload());
        Assertions.assertEquals(OutboxEventStatus.PENDING, outboxEvent.status());
        Assertions.assertEquals(createdAt, outboxEvent.createdAt());
        Assertions.assertNull(outboxEvent.publishedAt());
    }

    @Test
    void shouldPreservePublishedEventWhenMapping() {

        // Given
        OutboxEvent publishedEvent = OutboxEvent.pending(
                OutboxEventId.generate(),
                "booking-123",
                "PaymentCompleted",
                UUID.randomUUID(),
                "payment-completed",
                """
                {
                    "paymentId": "123",
                    "status": "COMPLETED"
                }
                """
        ).markPublished();

        // When
        OutboxJpaEntity entity = OutboxMapper.toEntity(publishedEvent);
        OutboxEvent restoredEvent = OutboxMapper.toDomain(entity);

        // Then
        Assertions.assertEquals(OutboxEventStatus.PUBLISHED, entity.getStatus());
        Assertions.assertNotNull(entity.getPublishedAt());

        Assertions.assertEquals(publishedEvent.id(), restoredEvent.id());
        Assertions.assertEquals(publishedEvent.messageKey(), restoredEvent.messageKey());
        Assertions.assertEquals(publishedEvent.eventType(), restoredEvent.eventType());
        Assertions.assertEquals(publishedEvent.aggregateId(), restoredEvent.aggregateId());
        Assertions.assertEquals(publishedEvent.topic(), restoredEvent.topic());
        Assertions.assertEquals(publishedEvent.payload(), restoredEvent.payload());
        Assertions.assertEquals(publishedEvent.status(), restoredEvent.status());
        Assertions.assertEquals(publishedEvent.createdAt(), restoredEvent.createdAt());
        Assertions.assertEquals(publishedEvent.publishedAt(), restoredEvent.publishedAt());

        // Verificar que la conversión no altera el objeto original
        Assertions.assertEquals(publishedEvent, restoredEvent);
    }
}