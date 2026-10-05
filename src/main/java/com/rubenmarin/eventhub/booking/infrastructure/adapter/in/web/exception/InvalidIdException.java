package com.rubenmarin.eventhub.booking.infrastructure.adapter.in.web.exception;

public class InvalidIdException extends IllegalArgumentException{

    public InvalidIdException(String message){
        super(message);
    }
}
