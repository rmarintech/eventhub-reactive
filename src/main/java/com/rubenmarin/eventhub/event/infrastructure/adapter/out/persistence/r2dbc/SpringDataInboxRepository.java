package com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface SpringDataInboxRepository extends ReactiveCrudRepository<InboxEntity, UUID> {
}
