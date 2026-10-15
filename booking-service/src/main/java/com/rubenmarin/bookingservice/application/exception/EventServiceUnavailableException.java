package com.rubenmarin.bookingservice.application.exception;

public class EventServiceUnavailableException extends RuntimeException {
    public EventServiceUnavailableException(Throwable cause) {
        super("Event Service is temporarily unavailable", cause);
    }
}
