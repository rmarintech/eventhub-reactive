package com.rubenmarin.paymentservice.application.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
/**
 * - aggregateId: identifica el agregado al que pertenece el evento.
 * - messageKey: determina la partición de Kafka a la que se enviará el mensaje, utilizando el particionador habitual.
 *      En nuestro caso, para eventos PaymentCompleted y PaymentFailed:
 *          aggregateId = payment.getPaymentId().paymentId();
 *          messageKey  = payment.getBookingId().bookingId().toString();
 *      Esto permite que los eventos del mismo Payment se envíen a la misma partición,
 *      siempre que no cambie el particionado del topic.
 * */
public record OutboxEvent(
        OutboxEventId id,
        String messageKey,
        String eventType,
        UUID aggregateId,
        String topic,
        String payload,
        OutboxEventStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime publishedAt
) {
    public OutboxEvent {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(messageKey, "messageKey cannot be null");
        Objects.requireNonNull(eventType, "eventType cannot be null");
        Objects.requireNonNull(aggregateId, "aggregateId cannot be null");
        Objects.requireNonNull(topic, "topic cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");

        if (messageKey.isBlank()){
            throw new IllegalArgumentException("messageKey cannot be blank");
        }
        if (eventType .isBlank()){
            throw new IllegalArgumentException("eventType  cannot be blank");
        }
        if (topic .isBlank()){
            throw new IllegalArgumentException("topic  cannot be blank");
        }

        if (status == OutboxEventStatus.PENDING && publishedAt != null) {
            throw new IllegalArgumentException("Pending events cannot have a publication date");
        }

        if (status == OutboxEventStatus.PUBLISHED && publishedAt == null) {
            throw new IllegalArgumentException("Published events must have a publication date");
        }

    }


    public static OutboxEvent pending(
            String messageKey,
            String eventType,
            UUID aggregateId,
            String topic,
            String payload) {

        return new OutboxEvent(
                new OutboxEventId(UUID.randomUUID()),
                 messageKey,
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
                messageKey,
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
