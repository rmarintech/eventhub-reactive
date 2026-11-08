package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.outbox;

import com.rubenmarin.paymentservice.application.model.OutboxEventStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxJpaEntity {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "message_key", nullable = false)
    private String messageKey;
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;
    @Column(nullable = false)
    private String topic;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OutboxEventStatus status;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    protected OutboxJpaEntity() {
    }

    public OutboxJpaEntity(
            UUID id,
            String messageKey,
            String eventType,
            UUID aggregateId,
            String topic,
            String payload,
            OutboxEventStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime publishedAt) {

        this.id = id;
        this.messageKey = messageKey;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.topic = topic;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getTopic() {
        return topic;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxEventStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }
}
