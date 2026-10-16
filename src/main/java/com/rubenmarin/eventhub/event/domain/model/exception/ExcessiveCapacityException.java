package com.rubenmarin.eventhub.event.domain.model.exception;

public class ExcessiveCapacityException extends RuntimeException {

    public ExcessiveCapacityException(String message) {
        super(message);
    }
}
