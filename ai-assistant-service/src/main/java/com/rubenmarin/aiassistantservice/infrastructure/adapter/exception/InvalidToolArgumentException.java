package com.rubenmarin.aiassistantservice.infrastructure.adapter.exception;

public class InvalidToolArgumentException extends RuntimeException {
    public InvalidToolArgumentException(String eventId) {
        super("Invalid event ID: '" + eventId + "'. A valid UUID is required.");
    }
}
