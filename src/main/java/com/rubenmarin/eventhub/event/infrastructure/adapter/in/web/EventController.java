package com.rubenmarin.eventhub.event.infrastructure.adapter.in.web;

import com.rubenmarin.eventhub.event.application.port.in.*;
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
    private final PublishEventUseCase publishEventUseCase;
    private final ReserveEventPlacesUseCase reserveEventPlacesUseCase;
    private final ReleaseEventPlacesUseCase releaseEventPlacesUseCase;

    public EventController(
            CreateEventUseCase createEventUseCase,
            EventQueryUseCase eventQueryUseCase,
            PublishEventUseCase publishEventUseCase,
            ReserveEventPlacesUseCase reserveEventPlacesUseCase,
            ReleaseEventPlacesUseCase releaseEventPlacesUseCase) {
        this.createEventUseCase = createEventUseCase;
        this.eventQueryUseCase = eventQueryUseCase;
        this.publishEventUseCase = publishEventUseCase;
        this.reserveEventPlacesUseCase = reserveEventPlacesUseCase;
        this.releaseEventPlacesUseCase = releaseEventPlacesUseCase;
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

    @PostMapping("/{id}/publish")
    @ResponseStatus(HttpStatus.OK)
    public Mono<EventResponse> publishEvent(@PathVariable String id) {

        Mono<Event> event = publishEventUseCase.publishEvent(parseEventId(id));
        Mono<EventResponse> response = event.map(value -> toResponse(value));
        return response;
    }

    @PostMapping("/{id}/reserve/{places}/places")
    @ResponseStatus(HttpStatus.OK)
    public Mono<EventResponse> reserveEventPlaces(
            @PathVariable String id,
            @PathVariable int places) {

        Mono<Event> reservedEvent =
                reserveEventPlacesUseCase.reserveEventPlaces(
                        parseEventId(id),
                        places
                );

        Mono<EventResponse> response = reservedEvent.map(event -> toResponse(event));

        return response;
    }

    @PostMapping("/{id}/release/{places}/places")
    @ResponseStatus(HttpStatus.OK)
    public Mono<EventResponse> releaseEventPlaces(
            @PathVariable String id,
            @PathVariable int places) {

        Mono<Event> eventReleased =
                releaseEventPlacesUseCase.releaseEventPlaces(
                        parseEventId(id),
                        places
                );

        Mono<EventResponse> response = eventReleased.map(event -> toResponse(event));

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
                event.capacity().available(),
                event.price().amount(),
                event.price().currency().getCurrencyCode(),
                event.status().name()
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
