package com.rubenmarin.eventhub.shared.application.handler;

import com.rubenmarin.eventhub.shared.domain.event.DomainEvent;
import reactor.core.publisher.Mono;

//T = Domain Event concreto que sé manejar
public interface DomainEventHandler<T extends DomainEvent> {

    //qué hago cuando lo recibo
    Mono<Void> handle(T event);
// qué tipo de evento sé manejar
    Class<T> eventType();
}