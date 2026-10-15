package com.rubenmarin.bookingservice.application.exception;

import com.rubenmarin.bookingservice.domain.model.EventId;

public class EventNotAvailableException extends RuntimeException {

    public EventNotAvailableException(EventId eventId) {
        super("Event not available: " + eventId.eventId());
    }
}