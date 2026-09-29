package com.rubenmarin.eventhub.event.application.service;

import com.rubenmarin.eventhub.event.application.port.in.CreateEventCommand;
import com.rubenmarin.eventhub.event.application.port.in.CreateEventUseCase;
import com.rubenmarin.eventhub.event.application.port.out.EventRepository;
import com.rubenmarin.eventhub.event.domain.model.Capacity;
import com.rubenmarin.eventhub.event.domain.model.Event;
import com.rubenmarin.eventhub.event.domain.model.EventName;
import com.rubenmarin.eventhub.event.domain.model.Money;

import java.time.LocalDateTime;

public class CreateEventService implements CreateEventUseCase {

    private final EventRepository eventRepository;

    public CreateEventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public Event createEvent(CreateEventCommand command) {

            Event event = Event.create(
                    new EventName(command.name()),
                    command.description(),
                    command.startDate(),
                     Capacity.of(command.capacity()),
                    new Money(command.price(), command.currency())

            );

            Event saved = eventRepository.save(event);
            return saved;
        }
    }
