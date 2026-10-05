package com.rubenmarin.eventhub.event.application.port.in;

import com.rubenmarin.eventhub.event.domain.model.Event;
import com.rubenmarin.eventhub.event.domain.model.EventId;
import reactor.core.publisher.Mono;

/**
 * Inbound port for the Publish Event use case.
 *
 * It defines what the application allows an external actor
 * to request without exposing any infrastructure technology.
 */
public interface PublishEventUseCase {

    Mono<Event> publishEvent(EventId id);
}