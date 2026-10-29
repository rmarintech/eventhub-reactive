package com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery;

import com.rubenmarin.eventhub.event.infrastructure.recovery.RecoveryStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class EventRecoveryEntityTest {

    private EventRecoveryEntity entity;
    private EventRecoveryEntityId id;

    @BeforeEach
    void setUp() {
        id = new EventRecoveryEntityId(
                "booking-created.DLT",
                0,
                25L
        );

        entity = new EventRecoveryEntity(
                id,
                "booking-123",
                "{}",
                "{\"integrationEventId\":\"550e8400-e29b-41d4-a716-446655440000\"}"
        );
    }

    @Test
    void shouldCreateRecoveryAsPending() {
        Assertions.assertEquals(RecoveryStatus.PENDING, entity.getStatus());
        Assertions.assertEquals(id, entity.getEventRecoveryEntityId());

        Assertions.assertNotNull(entity.getPendingAt());
        Assertions.assertNull(entity.getProcessingAt());
        Assertions.assertNull(entity.getRepublishedAt());
        Assertions.assertNull(entity.getAttemptId());
        Assertions.assertNull(entity.getVersion());

        Assertions.assertEquals(
                "{\"integrationEventId\":\"550e8400-e29b-41d4-a716-446655440000\"}",
                entity.getPayload()
        );
    }

    @Test
    void shouldMarkAsProcessing() {
        UUID attemptId = UUID.randomUUID();

        entity.markAsProcessing(attemptId);

        Assertions.assertEquals(RecoveryStatus.PROCESSING, entity.getStatus());
        Assertions.assertEquals(attemptId, entity.getAttemptId());
        Assertions.assertNotNull(entity.getProcessingAt());
        Assertions.assertNull(entity.getRepublishedAt());
    }

    @Test
    void shouldRejectProcessingWhenNotPending() {
        UUID attemptId = UUID.randomUUID();

        entity.markAsProcessing(attemptId);

        Assertions.assertThrows(
                IllegalStateException.class,
                () -> entity.markAsProcessing(UUID.randomUUID())
        );

        entity.markAsRepublished(attemptId);

        Assertions.assertThrows(
                IllegalStateException.class,
                () -> entity.markAsProcessing(UUID.randomUUID())
        );
    }

    @Test
    void shouldRejectNullAttemptId() {
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> entity.markAsProcessing(null)
        );

        Assertions.assertEquals(RecoveryStatus.PENDING, entity.getStatus());
    }

    @Test
    void shouldMarkAsRepublished() {
        UUID attemptId = UUID.randomUUID();

        entity.markAsProcessing(attemptId);
        entity.markAsRepublished(attemptId);

        Assertions.assertEquals(RecoveryStatus.REPUBLISHED, entity.getStatus());
        Assertions.assertNotNull(entity.getRepublishedAt());
        Assertions.assertEquals(attemptId, entity.getAttemptId());
    }

    @Test
    void shouldRejectRepublishingWhenNotProcessing() {
        UUID attemptId = UUID.randomUUID();

        // PENDING → REPUBLISHED no está permitido
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> entity.markAsRepublished(attemptId)
        );

        // REPUBLISHED → REPUBLISHED tampoco está permitido
        entity.markAsProcessing(attemptId);
        entity.markAsRepublished(attemptId);

        Assertions.assertThrows(
                IllegalStateException.class,
                () -> entity.markAsRepublished(attemptId)
        );
    }

    @Test
    void shouldRejectRepublishingWithDifferentAttemptId() {
        UUID originalAttemptId = UUID.randomUUID();
        UUID differentAttemptId = UUID.randomUUID();

        entity.markAsProcessing(originalAttemptId);

        Assertions.assertThrows(
                IllegalStateException.class,
                () -> entity.markAsRepublished(differentAttemptId)
        );

        // El estado no debe modificarse
        Assertions.assertEquals(RecoveryStatus.PROCESSING, entity.getStatus());
        Assertions.assertEquals(originalAttemptId, entity.getAttemptId());
        Assertions.assertNull(entity.getRepublishedAt());
    }

    @Test
    void shouldResetProcessingToPending() {
        UUID attemptId = UUID.randomUUID();

        entity.markAsProcessing(attemptId);
        entity.markAsPendingFromProcessing();

        Assertions.assertEquals(RecoveryStatus.PENDING, entity.getStatus());
        Assertions.assertNotNull(entity.getPendingAt());
        Assertions.assertNull(entity.getProcessingAt());
        Assertions.assertNull(entity.getAttemptId());
        Assertions.assertNull(entity.getRepublishedAt());
    }

    @Test
    void shouldRejectResetWhenNotProcessing() {
        // PENDING → PENDING
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> entity.markAsPendingFromProcessing()
        );

        UUID attemptId = UUID.randomUUID();

        entity.markAsProcessing(attemptId);
        entity.markAsRepublished(attemptId);

        // REPUBLISHED → PENDING
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> entity.markAsPendingFromProcessing()
        );

        Assertions.assertEquals(RecoveryStatus.REPUBLISHED, entity.getStatus());
    }
}