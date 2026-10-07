package com.rubenmarin.eventhub.shared.infrastructure.event;

import com.rubenmarin.eventhub.shared.application.event.DomainEventDispatcher;
import com.rubenmarin.eventhub.shared.application.port.out.DomainEventPublisher;
import com.rubenmarin.eventhub.shared.domain.event.DomainEvent;
import reactor.core.publisher.Mono;

public class InMemoryDomainEventPublisher implements DomainEventPublisher {


    private final DomainEventDispatcher domainEventDispatcher;


    public InMemoryDomainEventPublisher (DomainEventDispatcher domainEventDispatcher){
        this.domainEventDispatcher = domainEventDispatcher;
    }
    @Override
    public Mono<Void> publishDomainEvent(DomainEvent event) {
        return domainEventDispatcher.dispatch(event);
    }
}
