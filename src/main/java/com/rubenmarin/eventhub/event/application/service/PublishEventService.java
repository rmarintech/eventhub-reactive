package com.rubenmarin.eventhub.event.application.service;

import com.rubenmarin.eventhub.event.application.exception.EventNotFoundException;
import com.rubenmarin.eventhub.event.application.port.in.PublishEventUseCase;
import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.domain.model.Event;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import reactor.core.publisher.Mono;

public class PublishEventService implements PublishEventUseCase {
    private final EventRepository eventRepository;

    public PublishEventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public Mono<Event> publishEvent(EventId id) {
        Mono<Event> eventFound = eventRepository.findById(id)
                .switchIfEmpty(
                        Mono.error(
                                new EventNotFoundException("Event not found")
                        )
                );

        Mono<Event> eventPublished =
                eventFound.flatMap(event -> {
                    event.publish();
                    return eventRepository.save(event);

                });

        return eventPublished;
    }
}
