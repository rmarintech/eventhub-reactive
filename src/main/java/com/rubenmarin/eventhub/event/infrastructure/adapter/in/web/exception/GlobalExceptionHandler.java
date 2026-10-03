package com.rubenmarin.eventhub.event.infrastructure.adapter.in.web.exception;

import com.rubenmarin.eventhub.event.application.exception.EventNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<?> handleEventNotFoundException(EventNotFoundException e){

        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    };

    @ExceptionHandler(InvalidEventIdException.class)
    public ResponseEntity<?> handleInvalidEventIdException(InvalidEventIdException e){

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }





}
