package com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery;

import java.util.Objects;

public record EventRecoveryEntityId(
        String kafkaTopic,
        Integer kafkaPartition,
        Long kafkaOffset
) {

    public EventRecoveryEntityId {
        Objects.requireNonNull(kafkaTopic);
        Objects.requireNonNull(kafkaPartition);
        Objects.requireNonNull(kafkaOffset);


        if (kafkaOffset<0) throw new IllegalArgumentException("Offset cannot be negative");
        if (kafkaPartition<0) throw new IllegalArgumentException("Partition cannot be negative");
        if (kafkaTopic.isBlank()) throw new IllegalArgumentException("Topic cannot be blank");
    }
}
