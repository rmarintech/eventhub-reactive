package com.rubenmarin.eventhub.event.infrastructure.adapter.in.web;

import com.rubenmarin.eventhub.event.application.port.in.CreateEventCommand;
import com.rubenmarin.eventhub.event.application.port.in.CreateEventUseCase;
import com.rubenmarin.eventhub.event.application.port.in.EventQueryUseCase;
import com.rubenmarin.eventhub.event.domain.model.Event;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import com.rubenmarin.eventhub.event.infrastructure.adapter.in.web.exception.InvalidEventIdException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Currency;
import java.util.UUID;

@RestController
@RequestMapping("/events")
public class EventController {


    private final CreateEventUseCase createEventUseCase;
    private final EventQueryUseCase eventQueryUseCase;

    public EventController(CreateEventUseCase createEventUseCase,
                           EventQueryUseCase eventQueryUseCase) {
        this.createEventUseCase = createEventUseCase;
        this.eventQueryUseCase = eventQueryUseCase;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest rq) {

        Mono<Event> event = createEventUseCase.createEvent(toCommand(rq));
        Mono<EventResponse> response = event.map(value -> toResponse(value));
        return response;
    }


    @GetMapping
    public Flux<EventResponse> getAllEvents() {

        Flux<Event> events = eventQueryUseCase.findAll();
        Flux<EventResponse> response = events.map(value -> toResponse(value));
        return response;
    }

    @GetMapping("/{id}")
    public Mono<EventResponse> getEventById(@PathVariable String id) {

        Mono<Event> event = eventQueryUseCase.findById(parseEventId(id));
        Mono<EventResponse> response = event.map(value -> toResponse(value));
        return response;
    }


    private CreateEventCommand toCommand(CreateEventRequest rq) {
        return new CreateEventCommand(
                rq.name(),
                rq.description(),
                rq.startDate(),
                rq.capacity(),
                rq.price(),
                Currency.getInstance(rq.currency())
        );
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(
                event.id().value(),
                event.name().value(),
                event.description(),
                event.startDate(),
                event.capacity().total(),
                event.price().amount(),
                event.price().currency().getCurrencyCode()
        );
    }

    private EventId parseEventId(String id) {
        try {
            return new EventId(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            throw new InvalidEventIdException("Not a valid uuid: " + id);
        }
    }
}
