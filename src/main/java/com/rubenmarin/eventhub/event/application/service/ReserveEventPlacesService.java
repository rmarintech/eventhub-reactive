package com.rubenmarin.eventhub.event.application.service;

import com.rubenmarin.eventhub.event.application.exception.EventNotFoundException;
import com.rubenmarin.eventhub.event.application.port.in.ReserveEventPlacesUseCase;
import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.domain.model.Event;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import reactor.core.publisher.Mono;

public class ReserveEventPlacesService implements ReserveEventPlacesUseCase {
    private final EventRepository eventRepository;

    public ReserveEventPlacesService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public Mono<Event> reserveEventPlaces(EventId id, int places) {
        Mono<Event> eventFound = eventRepository.findById(id)
                .switchIfEmpty(
                        Mono.error(
                                new EventNotFoundException("Event not found")
                        )
                );

        Mono<Event> eventReserved =
                eventFound.flatMap(event -> {
                    event.reservePlaces(places);
                    return eventRepository.update(event);

                });

        return eventReserved;
    }
}
