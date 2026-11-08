package com.rubenmarin.paymentservice.application.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Stream;

class OutboxEventTest {

    @Test
    void shouldCreatePendingOutboxEvent() {

        // Given
        UUID aggregateId = UUID.randomUUID();
        String messageKey = UUID.randomUUID().toString();
        String eventType = "PaymentCompleted";
        String topic = "payment-completed";
        String payload = """
                {
                    "paymentId": "%s",
                    "status": "COMPLETED"
                }
                """.formatted(aggregateId);

        // When
        OutboxEvent outboxEvent = OutboxEvent.pending(
                messageKey,
                eventType,
                aggregateId,
                topic,
                payload
        );

        // Then
        Assertions.assertNotNull(outboxEvent.id());
        Assertions.assertNotNull(outboxEvent.id().id());

        Assertions.assertEquals(OutboxEventStatus.PENDING, outboxEvent.status());

        Assertions.assertNotNull(outboxEvent.createdAt());
        Assertions.assertNull(outboxEvent.publishedAt());

        Assertions.assertEquals(messageKey, outboxEvent.messageKey());
        Assertions.assertEquals(eventType, outboxEvent.eventType());
        Assertions.assertEquals(aggregateId, outboxEvent.aggregateId());
        Assertions.assertEquals(topic, outboxEvent.topic());
        Assertions.assertEquals(payload, outboxEvent.payload());
    }

    @Test
    void shouldMarkPendingOutboxEventAsPublished() {

        // Given
        UUID aggregateId = UUID.randomUUID();
        String messageKey = UUID.randomUUID().toString();
        String eventType = "PaymentCompleted";
        String topic = "payment-completed";
        String payload = """
                {
                    "paymentId": "%s",
                    "status": "COMPLETED"
                }
                """.formatted(aggregateId);

        OutboxEvent pendingEvent = OutboxEvent.pending(
                messageKey,
                eventType,
                aggregateId,
                topic,
                payload
        );

        // When
        OutboxEvent publishedEvent = pendingEvent.markPublished();

        // Then
        Assertions.assertEquals(OutboxEventStatus.PUBLISHED, publishedEvent.status());

        Assertions.assertNotNull(publishedEvent.publishedAt());

        // El evento conserva sus datos originales
        Assertions.assertEquals(pendingEvent.id(), publishedEvent.id());
        Assertions.assertEquals(pendingEvent.messageKey(), publishedEvent.messageKey());
        Assertions.assertEquals(pendingEvent.eventType(), publishedEvent.eventType());
        Assertions.assertEquals(pendingEvent.aggregateId(), publishedEvent.aggregateId());
        Assertions.assertEquals(pendingEvent.topic(), publishedEvent.topic());
        Assertions.assertEquals(pendingEvent.payload(), publishedEvent.payload());
        Assertions.assertEquals(pendingEvent.createdAt(), publishedEvent.createdAt());

        // El evento original permanece inmutable
        Assertions.assertEquals(OutboxEventStatus.PENDING, pendingEvent.status());
        Assertions.assertNull(pendingEvent.publishedAt());

        // La publicación ocurre después de la creación
        Assertions.assertFalse(publishedEvent.publishedAt().isBefore(publishedEvent.createdAt())
        );
    }

    @Test
    void shouldRejectPublishingAlreadyPublishedOutboxEvent() {

        // Given
        UUID aggregateId = UUID.randomUUID();
        String messageKey = UUID.randomUUID().toString();
        String eventType = "PaymentCompleted";
        String topic = "payment-completed";
        String payload = """
                {
                    "paymentId": "%s",
                    "status": "COMPLETED"
                }
                """.formatted(aggregateId);

        OutboxEvent pendingEvent = OutboxEvent.pending(
                messageKey,
                eventType,
                aggregateId,
                topic,
                payload
        );

        OutboxEvent publishedEvent = pendingEvent.markPublished();

        // When & Then
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () -> publishedEvent.markPublished());

        Assertions.assertEquals("Only pending events can be published", exception.getMessage());

        // Verificar que el evento permanece publicado
        Assertions.assertEquals(OutboxEventStatus.PUBLISHED, publishedEvent.status());

        Assertions.assertNotNull(publishedEvent.publishedAt());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidNullFields")
    void shouldRejectNullRequiredFields(
            String scenario,
            String messageKey,
            String eventType,
            UUID aggregateId,
            String topic,
            String payload
    ) {

        Assertions.assertThrows(
                NullPointerException.class,
                () -> OutboxEvent.pending(
                        messageKey,
                        eventType,
                        aggregateId,
                        topic,
                        payload
                )
        );
    }

    static Stream<Arguments> invalidNullFields() {
        UUID aggregateId = UUID.randomUUID();
        return Stream.of(
                Arguments.of("Null messageKey", null, "PaymentCompleted", aggregateId, "payment-completed", "{}"),
                Arguments.of("Null eventType", "booking-123", null, aggregateId, "payment-completed", "{}"),
                Arguments.of("Null aggregateId", "booking-123", "PaymentCompleted", null, "payment-completed", "{}"),
                Arguments.of("Null topic", "booking-123", "PaymentCompleted", aggregateId, null, "{}"),
                Arguments.of("Null payload", "booking-123", "PaymentCompleted", aggregateId, "payment-completed", null)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidBlankFields")
    void shouldRejectBlankRequiredFields(
            String scenario,
            String messageKey,
            String eventType,
            String topic
    ) {

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> OutboxEvent.pending(
                        messageKey,
                        eventType,
                        UUID.randomUUID(),
                        topic,
                        "{}"
                )
        );
    }

    static Stream<Arguments> invalidBlankFields() {

        return Stream.of(
                Arguments.of("Empty messageKey", "", "PaymentCompleted", "payment-completed"),
                Arguments.of("Blank messageKey", "   ", "PaymentCompleted", "payment-completed"),

                Arguments.of("Empty eventType", "booking-123", "", "payment-completed"),
                Arguments.of("Blank eventType", "booking-123", "   ", "payment-completed"),

                Arguments.of("Empty topic", "booking-123", "PaymentCompleted", ""),
                Arguments.of("Blank topic", "booking-123", "PaymentCompleted", "   ")
        );
    }

    @Test
    void shouldRejectPendingEventWithPublicationDate() {

        // Given
        OffsetDateTime createdAt = OffsetDateTime.now();
        OffsetDateTime publishedAt = createdAt.plusSeconds(10);

        // When & Then
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxEvent(
                        new OutboxEventId(UUID.randomUUID()),
                        "booking-123",
                        "PaymentCompleted",
                        UUID.randomUUID(),
                        "payment-completed",
                        "{}",
                        OutboxEventStatus.PENDING,
                        createdAt,
                        publishedAt
                )
        );

        Assertions.assertEquals("Pending events cannot have a publication date", exception.getMessage());
    }

    @Test
    void shouldRejectPublishedEventWithoutPublicationDate() {

        // Given
        OffsetDateTime createdAt = OffsetDateTime.now();

        // When & Then
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxEvent(
                        new OutboxEventId(UUID.randomUUID()),
                        "booking-123",
                        "PaymentCompleted",
                        UUID.randomUUID(),
                        "payment-completed",
                        "{}",
                        OutboxEventStatus.PUBLISHED,
                        createdAt,
                        null
                )
        );

        Assertions.assertEquals("Published events must have a publication date", exception.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidConstructorFields")
    void shouldRejectNullConstructorFields(
            String scenario,
            OutboxEventId id,
            OutboxEventStatus status,
            OffsetDateTime createdAt
    ) {

        Assertions.assertThrows(
                NullPointerException.class,
                () -> new OutboxEvent(
                        id,
                        "booking-123",
                        "PaymentCompleted",
                        UUID.randomUUID(),
                        "payment-completed",
                        "{}",
                        status,
                        createdAt,
                        null
                )
        );
    }

    static Stream<Arguments> invalidConstructorFields() {

        OutboxEventId validId = new OutboxEventId(UUID.randomUUID());
        OffsetDateTime now = OffsetDateTime.now();

        return Stream.of(
                Arguments.of("Null id", null, OutboxEventStatus.PENDING, now),
                Arguments.of("Null status", validId, null, now),
                Arguments.of("Null createdAt", validId, OutboxEventStatus.PENDING, null)
        );
    }

}