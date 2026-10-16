
package com.rubenmarin.eventhub.event.application.service;

import com.rubenmarin.eventhub.event.application.exception.ConcurrentUpdateException;
import com.rubenmarin.eventhub.event.application.exception.EventNotFoundException;
import com.rubenmarin.eventhub.event.application.port.in.ReleaseEventPlacesUseCase;
import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.domain.model.Event;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import reactor.core.publisher.Mono;

public class ReleaseEventPlacesService implements ReleaseEventPlacesUseCase {
    private final EventRepository eventRepository;

    public ReleaseEventPlacesService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public Mono<Event> releaseEventPlaces(EventId id, int places) {
        Mono<Event> eventReleased=
                this.attemptReleasing(id, places)
                        .onErrorResume(ConcurrentUpdateException.class, error -> {
                            return this.attemptReleasing(id, places);
                        });

        return eventReleased;
    }

    private Mono<Event> attemptReleasing(EventId id, int places) {
        Mono<Event> eventFound = eventRepository.findById(id)
                .switchIfEmpty(
                        Mono.error(
                                new EventNotFoundException("Event not found")
                        )
                );

        Mono<Event> eventReleased =
                eventFound.flatMap(event -> {
                    event.releasePlaces(places);
                    return eventRepository.update(event);
                });


        return eventReleased;

    }
}
