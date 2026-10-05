package com.rubenmarin.eventhub.booking.infrastructure.adapter.out.persistence.r2dbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;


@Table("bookings")
public record BookingEntity(
        @Id
        UUID id,
        @Column("customer_id")
        UUID customerId,
        @Column("event_id")
        UUID eventId,

        int places,
        String status,

        @Transient //No lo queremos en la ddbb solo metadatos
        boolean isNew

) implements Persistable<UUID> {

        @Override
        public UUID getId() {
                return id;
        }
}
