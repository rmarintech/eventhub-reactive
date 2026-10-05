package com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence;

import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.domain.model.Event;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

//Ya no lo necesitamos @Repository
public class InMemoryEventRepository implements EventRepository {

    private final Map<EventId, Event> events = new HashMap<>();

    @Override
    public Mono<Event> create(Event event) {

        // Ssí el put sucede antes de que nadie se subscriba
        // events.put(event.id(), event);
        // return Mono.just(event);
        // Mmejor con fromSupplier para que el put suceda al subscribirse:

        // Como es en memoria, no hace falta boundedElastic()
        return Mono.fromSupplier(() -> {
            events.put(event.id(), event);
            return event;
        });


    }

    @Override
    public Mono<Event> update(Event event) {

        // Ssí el put sucede antes de que nadie se subscriba
        // events.put(event.id(), event);
        // return Mono.just(event);
        // Mmejor con fromSupplier para que el put suceda al subscribirse:

        // Como es en memoria, no hace falta boundedElastic()
        return Mono.fromSupplier(() -> {
            events.put(event.id(), event);
            return event;
        });


    }

    @Override
    public Mono<Event> findById(EventId id) {
       return Mono.defer(() -> Mono.justOrEmpty(events.get(id)));
    }

    @Override
    public Flux<Event> findAll() {
        //defer creates a lazy publisher
        return Flux.defer(() -> Flux.fromIterable(events.values()));
    }
}