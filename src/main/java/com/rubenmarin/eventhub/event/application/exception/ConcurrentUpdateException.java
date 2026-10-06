package com.rubenmarin.eventhub.event.application.exception;

public class ConcurrentUpdateException extends RuntimeException {

    public ConcurrentUpdateException(String message) {
        super(message);
    }
}
