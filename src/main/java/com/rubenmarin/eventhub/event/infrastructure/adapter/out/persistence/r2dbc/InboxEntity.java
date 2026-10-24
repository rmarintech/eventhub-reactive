package com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("inbox_events")
public record InboxEntity(
        @Id
        @Column("integration_event_id")
        UUID integrationEventId,
        @Column("processed_at")
        OffsetDateTime processedAt,

        @Transient //No lo queremos en la ddbb solo metadatos
        boolean isNew

) implements Persistable<UUID> {

    @Override
    public UUID getId() {
        return integrationEventId;
    }
}
