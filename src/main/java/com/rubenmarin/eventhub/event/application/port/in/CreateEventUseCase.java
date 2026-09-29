package com.rubenmarin.eventhub.event.application.port.in;

import com.rubenmarin.eventhub.event.domain.model.Event;

/**
 * Inbound port for the Create Event use case.
 *
 * It defines what the application allows an external actor
 * to request without exposing any infrastructure technology.
 */
public interface CreateEventUseCase {

    Event createEvent(CreateEventCommand command);
}