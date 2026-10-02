package com.rubenmarin.eventhub.event.application.service;

import com.rubenmarin.eventhub.event.application.port.in.CreateEventCommand;
import com.rubenmarin.eventhub.event.application.port.in.CreateEventUseCase;
import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.domain.model.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CreateEventService implements CreateEventUseCase {

    private final EventRepository eventRepository;

    public CreateEventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public Mono<Event> createEvent(CreateEventCommand command) {

        Event event = Event.create(
                new EventName(command.name()),
                command.description(),
                command.startDate(),
                Capacity.of(command.capacity()),
                new Money(command.price(), command.currency())

        );

        Mono<Event> saved = eventRepository.save(event);
        return saved;
    }


}
