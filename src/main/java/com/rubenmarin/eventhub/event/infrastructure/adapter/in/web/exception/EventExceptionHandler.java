package com.rubenmarin.eventhub.event.infrastructure.adapter.in.web.exception;

import com.rubenmarin.eventhub.event.application.exception.ConcurrentUpdateException;
import com.rubenmarin.eventhub.event.application.exception.EventNotFoundException;
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
public class EventExceptionHandler {


    @ExceptionHandler(ConcurrentUpdateException.class)
    public ResponseEntity<?> handleConcurrentUpdateException(ConcurrentUpdateException e){

        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    };

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<?> handleEventNotFoundException(EventNotFoundException e){

        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    };

    @ExceptionHandler(InvalidEventIdException.class)
    public ResponseEntity<?> handleInvalidEventIdException(InvalidEventIdException e){

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ValidationErrorResponse> handleWebExchangeBindException(WebExchangeBindException ex){

        // LinkedHashMap preserves the order in which validation errors are found.
        Map<String, String> errors = new LinkedHashMap<>();

        // Extract every field that failed validation.
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }



        // Build our custom validation error response.
        ValidationErrorResponse response = new ValidationErrorResponse(
                Instant.now().toString(),
                HttpStatus.BAD_REQUEST.value(),
                ExceptionMsg.VALIDATION_FAILED,
                errors
        );


        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }




}
