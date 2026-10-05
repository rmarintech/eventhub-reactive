package com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface SpringDataEventRepository extends ReactiveCrudRepository<EventEntity, UUID> {
}
