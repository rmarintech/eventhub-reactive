package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.inbox;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "inbox_events")
public class InboxJpaEntity {

    @Id
    @Column(name = "integration_event_id", nullable = false, updatable = false)
    private UUID integrationEventId;
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    protected InboxJpaEntity() {
    }

    public InboxJpaEntity(UUID integrationEventId, Instant receivedAt) {
        this.integrationEventId = Objects.requireNonNull(integrationEventId);
        this.receivedAt = Objects.requireNonNull(receivedAt);
    }

    public UUID getIntegrationEventId() {
        return integrationEventId;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
