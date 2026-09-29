package com.rubenmarin.eventhub.event.application.port.out;

import com.rubenmarin.eventhub.event.domain.model.Event;

/**
 * Outbound port used by the application to persist Events.
 *
 * The application defines what it needs from persistence
 * without depending on a specific database technology.
 */
public interface EventRepository {

    Event save(Event event);
}