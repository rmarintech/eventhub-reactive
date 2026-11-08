package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.outbox;

import com.rubenmarin.paymentservice.application.model.OutboxEvent;
import com.rubenmarin.paymentservice.application.model.OutboxEventStatus;
import com.rubenmarin.paymentservice.application.port.out.OutboxRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@Testcontainers
class JpaOutboxRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private SpringDataOutboxRepository springDataOutboxRepository;

    @Test
    void shouldSaveAndRetrievePendingOutboxEvent() {

        // Given
        OutboxEvent event = OutboxEvent.pending(
                UUID.randomUUID().toString(),
                "PaymentCompleted",
                UUID.randomUUID(),
                "payment-completed",
                """
                        {
                            "status": "COMPLETED"
                        }
                        """
        );

        // When
        OutboxEvent savedEvent = outboxRepository.save(event);

        OutboxJpaEntity persisted = springDataOutboxRepository
                .findById(savedEvent.id().id())
                .orElseThrow();

        // Then
        Assertions.assertEquals(event.id().id(), persisted.getId());
        Assertions.assertEquals(event.messageKey(), persisted.getMessageKey());
        Assertions.assertEquals(event.eventType(), persisted.getEventType());
        Assertions.assertEquals(event.aggregateId(), persisted.getAggregateId());
        Assertions.assertEquals(event.topic(), persisted.getTopic());
        Assertions.assertEquals(event.payload(), persisted.getPayload());

        Assertions.assertEquals(OutboxEventStatus.PENDING, persisted.getStatus());
        Assertions.assertNull(persisted.getPublishedAt());

        Assertions.assertEquals(
                event.createdAt().toInstant().truncatedTo(ChronoUnit.MICROS),
                persisted.getCreatedAt().toInstant()
        );
    }

    @Test
    void shouldPersistPublishedOutboxEvent() {

        // Given
        OutboxEvent pendingEvent = OutboxEvent.pending(
                UUID.randomUUID().toString(),
                "PaymentCompleted",
                UUID.randomUUID(),
                "payment-completed",
                "{}"
        );

        outboxRepository.save(pendingEvent);

        // When
        OutboxEvent publishedEvent = pendingEvent.markPublished();

        outboxRepository.save(publishedEvent);

        OutboxJpaEntity persisted = springDataOutboxRepository
                .findById(pendingEvent.id().id())
                .orElseThrow();

        // Then
        Assertions.assertEquals(pendingEvent.id().id(), persisted.getId());
        Assertions.assertEquals(OutboxEventStatus.PUBLISHED, persisted.getStatus());
        Assertions.assertNotNull(persisted.getPublishedAt());

        Instant expectedPublished = publishedEvent.publishedAt().toInstant();
        Instant actualPublished = persisted.getPublishedAt().toInstant();

        Assertions.assertTrue(
                Math.abs(ChronoUnit.NANOS.between(expectedPublished, actualPublished)) <= 1_000,
                "Persisted publishedAt differs by more than one microsecond"
        );

        Instant expectedCreated = publishedEvent.createdAt().toInstant();
        Instant actualCreated = persisted.getCreatedAt().toInstant();

        Assertions.assertTrue(
                Math.abs(ChronoUnit.NANOS.between(expectedCreated, actualCreated)) <= 1_000,
                "Persisted publishedAt differs by more than one microsecond"
        );

    }

    @Test
    void shouldFindPendingEventsOrderedByCreatedAt() {

        // Given
        OffsetDateTime baseTime = OffsetDateTime.now()                .truncatedTo(ChronoUnit.MICROS);

        OutboxEvent olderEvent = new OutboxEvent(
                new com.rubenmarin.paymentservice.application.model.OutboxEventId(UUID.randomUUID()),
                UUID.randomUUID().toString(),
                "PaymentCompleted",
                UUID.randomUUID(),
                "payment-completed",
                "{}",
                OutboxEventStatus.PENDING,
                baseTime.minusMinutes(10),
                null
        );

        OutboxEvent newerEvent = new OutboxEvent(
                new com.rubenmarin.paymentservice.application.model.OutboxEventId(UUID.randomUUID()),
                UUID.randomUUID().toString(),
                "PaymentFailed",
                UUID.randomUUID(),
                "payment-failed",
                "{}",
                OutboxEventStatus.PENDING,
                baseTime.minusMinutes(5),
                null
        );

        OutboxEvent publishedEvent = OutboxEvent.pending(
                UUID.randomUUID().toString(),
                "PaymentCompleted",
                UUID.randomUUID(),
                "payment-completed",
                "{}"
        ).markPublished();

        // Insertamos en un orden distinto al cronológico
        outboxRepository.save(newerEvent);
        outboxRepository.save(publishedEvent);
        outboxRepository.save(olderEvent);

        // When
        List<OutboxEvent> pendingEvents = outboxRepository.findPendingEvents();

        // Filtramos los eventos creados en este test para evitar
        // interferencias de otros tests que utilicen la misma base de datos.
        List<OutboxEvent> testEvents = pendingEvents.stream()
                .filter(event ->
                        event.id().equals(olderEvent.id())
                                || event.id().equals(newerEvent.id()))
                .toList();

        // Then
        Assertions.assertEquals(2, testEvents.size());

        Assertions.assertEquals(olderEvent.id(), testEvents.get(0).id());
        Assertions.assertEquals(newerEvent.id(), testEvents.get(1).id());

        Assertions.assertEquals(OutboxEventStatus.PENDING, testEvents.get(0).status());
        Assertions.assertEquals(OutboxEventStatus.PENDING, testEvents.get(1).status());

        Assertions.assertFalse(pendingEvents.stream().anyMatch(event -> event.id().equals(publishedEvent.id())));
    }
}