package com.rubenmarin.bookingservice.application.exception;

import com.rubenmarin.bookingservice.domain.model.EventId;

public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(EventId eventId, Throwable cause) {
        super("Event not found: " + eventId.eventId(), cause);
    }
}