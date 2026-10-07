package com.rubenmarin.eventhub.shared.application.port.out;

import com.rubenmarin.eventhub.shared.domain.event.DomainEvent;
import reactor.core.publisher.Mono;

public interface DomainEventPublisher {

    Mono<Void> publishDomainEvent(DomainEvent event);
}
