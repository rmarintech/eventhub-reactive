package com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery;

import com.rubenmarin.eventhub.event.infrastructure.recovery.RecoveryStatus;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface SpringDataEventRecoveryRepository extends ReactiveCrudRepository<EventRecoveryEntity, EventRecoveryEntityId>{

    Flux<EventRecoveryEntity> findAllByStatus(RecoveryStatus status);
}
