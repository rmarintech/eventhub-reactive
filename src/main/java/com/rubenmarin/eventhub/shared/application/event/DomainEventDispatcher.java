package com.rubenmarin.eventhub.shared.application.event;

import com.rubenmarin.eventhub.shared.application.handler.DomainEventHandler;
import com.rubenmarin.eventhub.shared.domain.event.DomainEvent;
import reactor.core.publisher.Mono;

import java.util.List;

/*
“Sé que maneja algún DomainEvent, pero no sé cuál.”
Nosotros comprobamos primero:
domainEvent.getClass().equals(domainEventHandler.eventType())
Solo entonces hacemos el cast controlado.

event.getClass()
        ↓
BookingCreated.class

handler.eventType()
        ↓
BookingCreated.class

MATCH ✅
 */

public class DomainEventDispatcher {

    private final List<DomainEventHandler<?>> domainEventHandlers;

    public DomainEventDispatcher(
            List<DomainEventHandler<?>> domainEventHandlers) {

        this.domainEventHandlers = domainEventHandlers;
    }

    public Mono<Void> dispatch(DomainEvent domainEvent) {

        for (DomainEventHandler<?> domainEventHandler : domainEventHandlers) {

            if (domainEvent.getClass()
                    .equals(domainEventHandler.eventType())) {

                return handle(domainEventHandler, domainEvent);
            }
        }

        return Mono.empty();
    }

    @SuppressWarnings("unchecked")
    private <T extends DomainEvent> Mono<Void> handle(
            DomainEventHandler<?> handler,
            DomainEvent event) {

        DomainEventHandler<T> typedHandler =
                (DomainEventHandler<T>) handler;

        return typedHandler.handle((T) event);
    }
}