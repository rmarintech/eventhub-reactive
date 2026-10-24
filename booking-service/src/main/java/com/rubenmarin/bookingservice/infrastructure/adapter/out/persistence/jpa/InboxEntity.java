package com.rubenmarin.bookingservice.infrastructure.adapter.out.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "inbox_events")
public class InboxEntity {

    @Id
    @Column(name="integration_event_id")
    UUID integrationEventId;
    @Column(name = "processed_at")
    OffsetDateTime processedAt;

    protected InboxEntity() {
    }

    public InboxEntity(UUID integrationEventId, OffsetDateTime processedAt) {
        this.integrationEventId = integrationEventId;
        this.processedAt = processedAt;
    }

    public UUID getIntegrationEventId() {
        return integrationEventId;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }
}
