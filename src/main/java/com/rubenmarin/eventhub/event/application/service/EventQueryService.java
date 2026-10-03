package com.rubenmarin.eventhub.event.application.service;

import com.rubenmarin.eventhub.event.application.exception.EventNotFoundException;
import com.rubenmarin.eventhub.event.application.port.in.EventQueryUseCase;
import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.domain.model.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

public class EventQueryService implements EventQueryUseCase {

    private final EventRepository eventRepository;

    public EventQueryService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }


    @Override
    public Mono<Event> findById(EventId id) {
        Mono<Event> event = eventRepository.findById(id)
                .switchIfEmpty(
                        Mono.error(
                                new EventNotFoundException("Event not found")
                        )
                );
        return event;
    }

    @Override
    public Flux<Event> findAll() {
        Flux<Event> events = eventRepository.findAll();
        return events;
    }
}
