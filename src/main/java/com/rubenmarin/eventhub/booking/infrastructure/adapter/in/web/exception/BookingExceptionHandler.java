package com.rubenmarin.eventhub.booking.infrastructure.adapter.in.web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class BookingExceptionHandler {


    @ExceptionHandler(InvalidIdException.class)
    public ResponseEntity<?> handleInvalidIdException(InvalidIdException e) {

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }


}
