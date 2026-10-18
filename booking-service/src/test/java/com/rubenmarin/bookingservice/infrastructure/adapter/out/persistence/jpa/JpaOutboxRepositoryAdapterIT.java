package com.rubenmarin.bookingservice.infrastructure.adapter.out.persistence.jpa;

import com.rubenmarin.bookingservice.application.model.OutboxEvent;
import com.rubenmarin.bookingservice.application.model.OutboxEventId;
import com.rubenmarin.bookingservice.application.model.OutboxEventStatus;
import com.rubenmarin.bookingservice.application.port.out.OutboxRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.OffsetDateTime;
import java.util.UUID;

@SpringBootTest
@Testcontainers
public class JpaOutboxRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    OutboxRepository repository;

    @Autowired
    EntityManager entityManager;

    @Test
    void shouldPersistAndPublishOutboxEvent() {

        OutboxEventId id = new OutboxEventId(UUID.randomUUID());
        String eventType = "Booking Event";
        UUID aggregateId = UUID.randomUUID();
        String topic = "booking-created";
        String payload = "";
        OutboxEventStatus status = OutboxEventStatus.PENDING;
        OffsetDateTime createdAt = OffsetDateTime.now();
        OffsetDateTime publishedAt = null;

        OutboxEvent newEvent = new OutboxEvent(
                id,
                eventType,
                aggregateId,
                topic, payload,
                status, createdAt,
                publishedAt
        );

        OutboxEvent createdEvent = repository.createOutboxEvent(newEvent);

        OutboxEvent publishedEvent = repository.markAsPublished(id);

        // Read the persisted state after the transaction committed
        entityManager.clear();

        OutboxJpaEntity persistedEntity = entityManager.find(OutboxJpaEntity.class, id.id());

        Assertions.assertAll(
                () -> Assertions.assertNotNull(persistedEntity),
                () -> Assertions.assertEquals(newEvent.id(), createdEvent.id()),
                () -> Assertions.assertEquals(OutboxEventStatus.PENDING, createdEvent.status()),
                () -> Assertions.assertEquals(OutboxEventStatus.PUBLISHED, publishedEvent.status()),
                () -> Assertions.assertNotNull(publishedEvent.publishedAt()),
                () -> Assertions.assertEquals(OutboxEventStatus.PUBLISHED, persistedEntity.getStatus()),
                () -> Assertions.assertNotNull(persistedEntity.getPublishedAt())
        );
    }
}
