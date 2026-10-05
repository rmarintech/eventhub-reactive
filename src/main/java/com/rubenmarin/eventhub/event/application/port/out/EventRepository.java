package com.rubenmarin.eventhub.event.application.port.out;

import com.rubenmarin.eventhub.event.domain.model.Event;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outbound port used by the application to persist Events.
 * <p>
 * The application defines what it needs from persistence
 * without depending on a specific database technology.
 */
public interface EventRepository {

    Mono<Event> create (Event event);

    Mono<Event> update(Event event);

    Mono<Event> findById(EventId id);

    Flux<Event> findAll();

}