package com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc;

import com.rubenmarin.eventhub.event.application.model.InboxEvent;
import com.rubenmarin.eventhub.event.application.port.out.InboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class R2dbcInboxRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    InboxRepository inboxRepository;

    @Test
    void shouldSaveInboxEvent() {

        UUID integrationEventId = UUID.randomUUID();

        InboxEvent event = new InboxEvent(
                integrationEventId,
                OffsetDateTime.now()
        );

        StepVerifier.create(inboxRepository.save(event))
                .assertNext(saved -> {
                    assertEquals(integrationEventId, saved.integrationEventId());
                    assertEquals(event.processedAt(), saved.processedAt());
                })
                .verifyComplete();

        // Verify the event was persisted in PostgreSQL.
        StepVerifier.create(inboxRepository.existsById(integrationEventId))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldRejectDuplicateInboxEvent() {

        UUID integrationEventId = UUID.randomUUID();

        InboxEvent first = new InboxEvent(
                integrationEventId,
                OffsetDateTime.now()
        );

        InboxEvent duplicate = new InboxEvent(
                integrationEventId,
                OffsetDateTime.now()
        );

        // First INSERT succeeds.
        StepVerifier.create(inboxRepository.save(first))
                .assertNext(saved ->
                        assertEquals(integrationEventId, saved.integrationEventId())
                )
                .verifyComplete();

        // Second INSERT violates the PostgreSQL primary key.
        StepVerifier.create(inboxRepository.save(duplicate))
                .expectError(org.springframework.dao.DuplicateKeyException.class)
                .verify();

        // The original event is still persisted.
        StepVerifier.create(inboxRepository.existsById(integrationEventId))
                .assertNext(exists -> assertTrue(exists))
                .verifyComplete();
    }
}