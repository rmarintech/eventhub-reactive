package com.rubenmarin.bookingservice.infrastructure.adapter.out.persistence.jpa;

import com.rubenmarin.bookingservice.application.model.BrokerName;
import com.rubenmarin.bookingservice.application.model.OutboxEvent;
import com.rubenmarin.bookingservice.application.model.OutboxEventId;
import com.rubenmarin.bookingservice.application.model.OutboxEventStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "broker_name", nullable = false)
    @Enumerated(EnumType.STRING)
    private BrokerName brokerName;

    @Column(name = "event_type", nullable = false)
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

    private OutboxJpaEntity(
            UUID id,
            BrokerName brokerName,
            String eventType,
            UUID aggregateId,
            String topic,
            String payload,
            OutboxEventStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime publishedAt) {

        this.id = id;
        this.brokerName = brokerName;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.topic = topic;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
        this.publishedAt = publishedAt;
    }

    public static OutboxJpaEntity fromModel(OutboxEvent event) {
        return new OutboxJpaEntity(
                event.id().id(),
                event.brokerName(),
                event.eventType(),
                event.aggregateId(),
                event.topic(),
                event.payload(),
                event.status(),
                event.createdAt(),
                event.publishedAt()
        );
    }

    public OutboxEvent toModel() {
        return new OutboxEvent(
                new OutboxEventId(id),
                brokerName,
                eventType,
                aggregateId,
                topic,
                payload,
                status,
                createdAt,
                publishedAt
        );
    }

    public void updateFromModel(OutboxEvent event) {
        this.status = event.status();
        this.publishedAt = event.publishedAt();
    }

    public UUID getId() {
        return id;
    }

    public BrokerName getBrokerName() {return brokerName;}

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
