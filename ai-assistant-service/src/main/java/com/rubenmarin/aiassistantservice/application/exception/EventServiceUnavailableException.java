package com.rubenmarin.aiassistantservice.application.exception;

import java.util.UUID;

public class EventServiceUnavailableException extends RuntimeException {

    public EventServiceUnavailableException() {
        super("Event Service is unavailable");
    }
    public EventServiceUnavailableException(Throwable cause) {
        super("Event service is not available" , cause);
    }
}