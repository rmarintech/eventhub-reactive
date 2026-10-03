package com.rubenmarin.eventhub.event.infrastructure.adapter.in.web.exception;

public class InvalidEventIdException extends IllegalArgumentException{

    public InvalidEventIdException (String message){
        super(message);
    }
}
