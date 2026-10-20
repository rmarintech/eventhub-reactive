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
    @Column(name = "event_id")
    UUID eventId;
    @Column(name = "processed_at")
    OffsetDateTime processedAt;

    protected InboxEntity() {
    }

    public InboxEntity(UUID eventId, OffsetDateTime processedAt) {
        this.eventId = eventId;
        this.processedAt = processedAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }
}
