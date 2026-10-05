package com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc;

import org.springframework.data.annotation.Id  ;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Table("events")
public record EventEntity(
        @Id
        UUID id,
        String name,
        String description,
        @Column("start_date")
        LocalDateTime startDate,
        @Column("capacity_total")
        int capacityTotal,
        @Column("capacity_available")
        int capacityAvailable,
        @Column("price_amount")
        BigDecimal priceAmount,
        @Column("price_currency")
        String priceCurrency,
        String status,

        @Transient //No lo queremos en la ddbb solo metadatos
        boolean isNew

) implements Persistable<UUID> {

        @Override
        public UUID getId() {
                return id;
        }
}
